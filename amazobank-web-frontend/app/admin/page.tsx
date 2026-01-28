"use client"

import { useEffect, useMemo, useState } from "react"
import { useRouter } from "next/navigation"
import { DashboardLayout } from "@/components/layout/dashboard-layout"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Users, UserPlus, Shield, Loader2 } from "lucide-react"
import { fetchUsers } from "@/services/userApi"
import { fetchRecentActivity } from "@/services/dashboardApi"

type ActivityItem = {
  action: string
  details: string
  time: string
  status?: string
}

export default function AdminDashboard() {
  const router = useRouter()
  const [userStats, setUserStats] = useState({
    totalUsers: 0,
    adminUsers: 0,
    agentUsers: 0,
  })
  const [loadingStats, setLoadingStats] = useState(true)
  const [activities, setActivities] = useState<ActivityItem[]>([])
  const [loadingActivity, setLoadingActivity] = useState(true)

  useEffect(() => {
    const loadUsers = async () => {
      setLoadingStats(true)
      try {
        const data = await fetchUsers()
        const totalUsers = data.length
        const adminUsers = data.filter((user) => ["Admin", "SuperAdmin"].includes(user.role ?? "")).length
        const agentUsers = data.filter((user) => user.role === "Agent").length
        setUserStats({ totalUsers, adminUsers, agentUsers })
      } catch (error) {
        console.error("Failed to load admin stats:", error)
        setUserStats({ totalUsers: 0, adminUsers: 0, agentUsers: 0 })
      } finally {
        setLoadingStats(false)
      }
    }

    const loadActivity = async () => {
      setLoadingActivity(true)
      try {
        const activity = await fetchRecentActivity()
        setActivities(activity)
      } catch (error) {
        console.error("Failed to load activity:", error)
        setActivities([])
      } finally {
        setLoadingActivity(false)
      }
    }

    void loadUsers()
    void loadActivity()
  }, [])

  const cards = useMemo(
    () => [
      {
        title: "Total Users",
        value: userStats.totalUsers,
        description: "Active system users",
        icon: Users,
      },
      {
        title: "Admin Users",
        value: userStats.adminUsers,
        description: "Administrator accounts",
        icon: Shield,
      },
      {
        title: "Agent Users",
        value: userStats.agentUsers,
        description: "Agent accounts",
        icon: UserPlus,
      },
    ],
    [userStats],
  )

  return (
    <DashboardLayout requiredRole="Admin">
      <div className="space-y-6">
        {/* Stats Grid */}
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {cards.map((stat) => {
            const Icon = stat.icon
            return (
              <Card key={stat.title}>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">{stat.title}</CardTitle>
                  <Icon className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    {loadingStats ? "…" : stat.value.toLocaleString()}
                  </div>
                  <p className="text-xs text-muted-foreground">{stat.description}</p>
                </CardContent>
              </Card>
            )
          })}
        </div>

        {/* Quick Actions */}
        <Card>
          <CardHeader>
            <CardTitle>Quick Actions</CardTitle>
            <CardDescription>Common administrative tasks</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid gap-4 md:grid-cols-2">
              <button
                type="button"
                onClick={() => router.push("/admin/users?create=1")}
                className="p-4 border border-border rounded-lg hover:bg-accent/50 transition-colors text-left"
              >
                <div className="flex items-center gap-3">
                  <UserPlus className="h-5 w-5 text-primary" />
                  <div>
                    <h3 className="font-medium">Create New User</h3>
                    <p className="text-sm text-muted-foreground">Add a new admin or agent user</p>
                  </div>
                </div>
              </button>
              <button
                type="button"
                onClick={() => router.push("/admin/users")}
                className="p-4 border border-border rounded-lg hover:bg-accent/50 transition-colors text-left"
              >
                <div className="flex items-center gap-3">
                  <Users className="h-5 w-5 text-primary" />
                  <div>
                    <h3 className="font-medium">Manage Users</h3>
                    <p className="text-sm text-muted-foreground">View and edit existing users</p>
                  </div>
                </div>
              </button>
            </div>
          </CardContent>
        </Card>

        {/* Recent Activity */}
        <Card>
          <CardHeader>
            <CardTitle>Recent Activity</CardTitle>
            <CardDescription>Latest system events and user actions</CardDescription>
          </CardHeader>
          <CardContent>
            {loadingActivity ? (
              <div className="flex items-center text-sm text-muted-foreground">
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Loading activity...
              </div>
            ) : activities.length === 0 ? (
              <p className="text-sm text-muted-foreground">No activity captured yet.</p>
            ) : (
              <div className="space-y-4">
                {activities.map((activity, index) => (
                  <div
                    key={`${activity.action}-${activity.time}-${index}`}
                    className="flex items-center justify-between py-2 border-b border-border last:border-0"
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className={`w-2 h-2 rounded-full ${
                          activity.status === "completed" ? "bg-green-500" : "bg-blue-500"
                        }`}
                      />
                      <div>
                        <p className="text-sm font-medium capitalize">{activity.action}</p>
                        <p className="text-xs text-muted-foreground">{activity.details}</p>
                      </div>
                    </div>
                    <span className="text-xs text-muted-foreground">{activity.time}</span>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  )
}
