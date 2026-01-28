import { getAccessToken } from "@/services/authToken"
import { getApiBaseUrl } from "@/services/apiBase"
import type { UserRole } from "@/types/roles"

const USER_API_BASE = `${getApiBaseUrl()}/api/users`
class UserApiError extends Error {
  status: number
  payload: unknown

  constructor(message: string, status: number, payload: unknown) {
    super(message)
    this.name = "UserApiError"
    this.status = status
    this.payload = payload
  }
}

// -------------------- COMMON HEADERS --------------------
function getAuthHeaders() {
  const token = getAccessToken()
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
}

// -------------------- GET USERS --------------------
export async function fetchUsers() {
  const res = await fetch(USER_API_BASE, {
    method: "GET",
    headers: getAuthHeaders(),
  })
  if (!res.ok) throw new Error("Failed to fetch users")
  return res.json()
}

// -------------------- CREATE USER --------------------
export async function createUser(userData: any) {
  const res = await fetch(USER_API_BASE, {
    method: "POST",
    headers: getAuthHeaders(),
    body: JSON.stringify({
      ...userData,
      password: "P@ssw0rd",
    }),
  })
  if (!res.ok) {
    const errorPayload = await res.text()
    let message = "Failed to create user"
    if (errorPayload) {
      try {
        const parsed = JSON.parse(errorPayload)
        if (typeof parsed?.error === "string") {
          message = parsed.error
        }
      } catch {
        message = errorPayload
      }
    }
    throw new Error(message)
  }
  return res.json()
}

function isJsonResponse(res: Response) {
  const contentType = res.headers.get("content-type")
  return contentType?.includes("application/json")
}

async function parseJsonBody(res: Response) {
  if (!isJsonResponse(res)) return null
  try {
    return await res.json()
  } catch {
    return null
  }
}

type DeleteUserSuccess = { result: "ok" }
type DeleteUserErrorPayload = { result: "err"; error: string }

export async function deleteUser(userId: string): Promise<DeleteUserSuccess> {
  const res = await fetch(`${USER_API_BASE}/${encodeURIComponent(userId)}`, {
    method: "DELETE",
    headers: getAuthHeaders(),
  })
  const payload = await parseJsonBody(res)

  if (res.status === 200 && payload?.result === "ok") {
    return payload as DeleteUserSuccess
  }

  if ((res.status === 403 || res.status === 404) && typeof (payload as DeleteUserErrorPayload)?.error === "string") {
    throw new UserApiError((payload as DeleteUserErrorPayload).error, res.status, payload)
  }

  throw new UserApiError("Failed to delete user.", res.status, payload)
}

export type UpdateUserPayload = {
  firstName: string
  lastName: string
  email: string
  password?: string
  role?: UserRole
}

export async function updateUser(userId: string, updates: UpdateUserPayload) {
  const body = {
    firstName: updates.firstName,
    lastName: updates.lastName,
    email: updates.email,
    ...(updates.password ? { password: updates.password } : {}),
    ...(updates.role ? { role: updates.role } : {}),
  }

  const res = await fetch(`${USER_API_BASE}/${encodeURIComponent(userId)}`, {
    method: "PATCH",
    headers: getAuthHeaders(),
    body: JSON.stringify(body),
  })

  const payload = await parseJsonBody(res)

  if (res.status === 200 && payload?.result === "ok") {
    return payload
  }

  if (res.status === 403 && typeof payload?.error === "string") {
    throw new UserApiError(payload.error, res.status, payload)
  }

  if ((res.status === 400 || res.status === 404 || res.status === 422) && typeof payload?.status === "string") {
    throw new UserApiError(payload.status, res.status, payload)
  }

  throw new UserApiError("Failed to update user.", res.status, payload)
}

export { UserApiError }
