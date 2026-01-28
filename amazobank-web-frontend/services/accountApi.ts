import { getAccessToken } from "@/services/authToken"
import { getApiBaseUrl } from "@/services/apiBase"

const API_BASE = `${getApiBaseUrl()}/api/accounts`

function getAuthHeaders() {
  const token = getAccessToken()
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
}

// Fetch accounts for a specific client
// accountApi.ts
export async function fetchAccounts(clientId?: string) {
  let url = API_BASE
  if (clientId) {
    url += `?clientId=${clientId}`
  }

  const res = await fetch(url, {
    method: "GET",
    headers: getAuthHeaders(),
  })

  if (!res.ok) throw new Error("Failed to fetch accounts")
  return res.json()
}

// Create a new account
export async function createAccount(accountData: any) {
  const res = await fetch(API_BASE, {
    method: "POST",
    headers: getAuthHeaders(),
    body: JSON.stringify(accountData),
  })

  if (!res.ok) throw new Error("Failed to create account")
  return res.json()
}

// Update account
export async function updateAccount(accountId: string, accountData: any) {
  const res = await fetch(`${API_BASE}/${accountId}`, {
    method: "PUT",
    headers: getAuthHeaders(),
    body: JSON.stringify(accountData),
  })

  if (!res.ok) throw new Error("Failed to update account")
  return res.json()
}

// Delete account
export async function deleteAccount(accountId: string) {
  const res = await fetch(`${API_BASE}/${accountId}`, {
    method: "DELETE",
    headers: getAuthHeaders(),
  })

  if (!res.ok) throw new Error("Failed to delete account")
  return res.json()
}
