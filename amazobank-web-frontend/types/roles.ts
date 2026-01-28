export type UserRole = "Agent" | "Admin" | "SuperAdmin"

export function canAccessAdminPortal(role: UserRole): boolean {
  return role === "Admin" || role === "SuperAdmin"
}
