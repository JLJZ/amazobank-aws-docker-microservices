import { NextResponse } from "next/server"
import type { UserRole } from "@/types/roles"

type BasicRole = Extract<UserRole, "Agent" | "Admin">

interface CreateUserPayload {
  firstName?: string
  lastName?: string
  email?: string
  role?: BasicRole
  requesterRole?: UserRole
}

interface UserResponse {
  id: string
  firstName: string
  lastName: string
  email: string
  role: BasicRole
  status: "Active" | "Disabled"
  createdAt: string
}

const users: UserResponse[] = []

export async function POST(request: Request) {
  const payload = (await request.json()) as CreateUserPayload
  const { firstName, lastName, email, role } = payload
  const requesterRole = payload.requesterRole ?? "Agent"

  if (!firstName || !lastName || !email || !role) {
    return NextResponse.json({ error: "Missing required fields." }, { status: 400 })
  }

  if (requesterRole === "Admin" && role === "Admin") {
    return NextResponse.json(
      { error: "Admins are not allowed to create other Admin users." },
      { status: 403 },
    )
  }

  const createdAt = new Date().toISOString()
  const newUser: UserResponse = {
    id: `usr_${Date.now()}`,
    firstName,
    lastName,
    email,
    role,
    status: "Active",
    createdAt,
  }

  users.push(newUser)

  return NextResponse.json(newUser, { status: 201 })
}

export async function GET() {
  return NextResponse.json(users, { status: 200 })
}
