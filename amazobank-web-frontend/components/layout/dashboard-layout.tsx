"use client"

import type React from "react"
import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Sidebar } from "./sidebar"
import type { UserRole } from "@/types/roles"
import { canAccessAdminPortal } from "@/types/roles"

interface User {
  UserID: string
  FirstName: string
  LastName: string
  Email: string
  Role: UserRole
}

interface DashboardLayoutProps {
  children: React.ReactNode
  requiredRole?: "Admin" | "Agent"
}

export function DashboardLayout({ children, requiredRole }: DashboardLayoutProps) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const router = useRouter()

  useEffect(() => {
    const storedUser = localStorage.getItem("user") // ✅ match login page

    if (!storedUser) {
      router.replace("/login")
      return
    }

    try {
      const userData = JSON.parse(storedUser) as User

      if (requiredRole) {
        const meetsRequirement =
          requiredRole === "Admin" ? canAccessAdminPortal(userData.Role) : userData.Role === requiredRole

        if (!meetsRequirement) {
          if (canAccessAdminPortal(userData.Role)) {
            router.replace("/admin")
          } else {
            router.replace("/agent")
          }
          return
        }
      }

      setUser(userData)
      setIsLoading(false)
    } catch (err) {
      console.error("Failed to parse stored user:", err)
      router.replace("/login")
    }
  }, [router, requiredRole])

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Loading...</p>
        </div>
      </div>
    )
  }

  if (!user) return null

  return (
    <div className="flex h-screen bg-background">
      <Sidebar userRole={user.Role} />
      <main className="flex-1 overflow-auto">
        <div className="p-6">
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-balance">Welcome back</h1>
            <p className="text-muted-foreground">{user.Role} Dashboard</p>
          </div>
          {children}
        </div>
      </main>
    </div>
  )
}
