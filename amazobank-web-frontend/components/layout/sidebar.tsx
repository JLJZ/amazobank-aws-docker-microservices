"use client"
import { useState } from "react"
import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Building2, Users, UserCheck, CreditCard, FileText, LogOut, Menu, X, Mail } from "lucide-react"
import { useAuth } from "react-oidc-context"
import type { UserRole } from "@/types/roles"

interface SidebarProps {
  userRole: UserRole
}

export function Sidebar({ userRole }: SidebarProps) {
  const [isCollapsed, setIsCollapsed] = useState(false)
  const pathname = usePathname()
  const router = useRouter()
  const auth = useAuth()

  const handleLogout = async () => {
    // Clear local storage
    localStorage.removeItem("user")
    localStorage.removeItem("token")
    localStorage.removeItem("idToken")
    localStorage.removeItem("accessToken")
    localStorage.removeItem("currentUser")

    try {
      // Get logout redirect URI from environment
      const logoutUri = process.env.NEXT_PUBLIC_COGNITO_LOGOUT_URI || window.location.origin + "/login"
      
      // Construct the proper Cognito logout URL
      const cognitoDomain = process.env.NEXT_PUBLIC_COGNITO_DOMAIN
      const clientId = process.env.NEXT_PUBLIC_COGNITO_CLIENT_ID
      
      const logoutUrl = 
        `https://${cognitoDomain}/logout?` +
        `client_id=${clientId}&` +
        `logout_uri=${encodeURIComponent(logoutUri)}`

      // Remove the auth state
      await auth.removeUser()
      
      // Redirect to Cognito logout endpoint
      window.location.href = logoutUrl
    } catch (error) {
      console.error("Failed to sign out from Cognito:", error)
      // Fallback to local logout
      router.push("/login")
    }
  }

  const adminNavItems = [
    { href: "/admin", label: "Dashboard", icon: Building2 },
    { href: "/admin/users", label: "Manage Users", icon: Users },
    { href: "/admin/logs", label: "Logs", icon: FileText },
  ]

  const agentNavItems = [
    { href: "/agent", label: "Dashboard", icon: Building2 },
    { href: "/agent/clients", label: "Clients", icon: UserCheck },
    { href: "/agent/accounts", label: "Accounts", icon: CreditCard },
    { href: "/agent/logs", label: "Logs", icon: FileText },
    { href: "/agent/llm-email", label: "LLM Email", icon: Mail },
  ]

  const navItems = userRole === "Agent" ? agentNavItems : adminNavItems

  return (
    <div
      className={cn(
        "flex flex-col h-screen bg-card border-r border-border transition-all duration-300",
        isCollapsed ? "w-16" : "w-64",
      )}
    >
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-border">
        {!isCollapsed && (
          <div className="flex items-center gap-2">
            <Building2 className="h-6 w-6 text-primary" />
            <span className="font-semibold">AmazoBank</span>
          </div>
        )}
        <Button variant="ghost" size="sm" onClick={() => setIsCollapsed(!isCollapsed)} className="h-8 w-8 p-0">
          {isCollapsed ? <Menu className="h-4 w-4" /> : <X className="h-4 w-4" />}
        </Button>
      </div>

      {/* Navigation */}
      <nav className="flex-1 p-4 space-y-2">
        {navItems.map((item) => {
          const Icon = item.icon
          const isActive = pathname === item.href
          return (
            <Link key={item.href} href={item.href}>
              <Button
                variant={isActive ? "secondary" : "ghost"}
                className={cn("w-full justify-start gap-3", isCollapsed && "px-2")}
              >
                <Icon className="h-4 w-4 flex-shrink-0" />
                {!isCollapsed && <span>{item.label}</span>}
              </Button>
            </Link>
          )
        })}
      </nav>

      {/* User Info & Logout */}
      <div className="p-4 border-t border-border">
        <Button
          variant="ghost"
          onClick={handleLogout}
          className={cn("w-full justify-start gap-3 text-destructive hover:text-destructive", isCollapsed && "px-2")}
        >
          <LogOut className="h-4 w-4 flex-shrink-0" />
          {!isCollapsed && <span>Logout</span>}
        </Button>
      </div>
    </div>
  )
}
