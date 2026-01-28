"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "react-oidc-context"
import assert from "assert"
import type { UserRole } from "@/types/roles"

export default function HomePage() {
  const router = useRouter()
  const { user } = useAuth()

  useEffect(() => {
    if (user) {
      // Normalize principal into User object
      const idToken = user?.id_token
      assert(idToken, "id_token should not be undefined")
      if (user?.access_token) {
        localStorage.setItem("accessToken", user.access_token)
      }
      if (idToken) {
        localStorage.setItem("idToken", idToken)
        localStorage.setItem("token", idToken)
      }

      const scopes = JSON.parse(atob(idToken.split(".")[1]))
      if (process.env.NODE_ENV === "development") {
        console.log("Scopes:", scopes)
      }

      const roleFromToken = (scopes["cognito:groups"]?.[0] ?? "Agent") as UserRole

      const normalizeUser = {
        UserID: scopes["sub"],
        Email: scopes["email"],
        FirstName: scopes["given_name"] || "<first_name>",
        LastName: scopes["family_name"] || "<last_name>",
        Role: roleFromToken,
      }
      localStorage.setItem("user", JSON.stringify(normalizeUser))

      const role = normalizeUser.Role
      // Redirect to appropriate dashboard
      if (role === "Admin" || role === "SuperAdmin") {
        router.push("/admin")
      } else if (role === "Agent") {
        router.push("/agent")
      } else {
        console.error("Unexpected role:", role)
        router.push("/")
      }
    } else {
      localStorage.removeItem("token")
      localStorage.removeItem("idToken")
      localStorage.removeItem("accessToken")
      localStorage.removeItem("user")
      router.push("/login")
    }
  }, [user])

  return (
    <div className="min-h-screen bg-background flex items-center justify-center">
      <div className="text-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
        <p className="text-muted-foreground">Redirecting...</p>
      </div>
    </div>
  )
}
