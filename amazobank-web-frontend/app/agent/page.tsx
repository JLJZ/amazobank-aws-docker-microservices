"use client"

import { useEffect, useState } from "react"
import { DashboardLayout } from "@/components/layout/dashboard-layout"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { UserCheck, CreditCard } from "lucide-react"
import { fetchDashboardStats, fetchRecentActivity } from "@/services/dashboardApi"

export default function AgentDashboard() {
    const [stats, setStats] = useState({
        totalClients: 0,
        totalAccounts: 0,
    })
    const [activities, setActivities] = useState<any[]>([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        const loadData = async () => {
            const [statsData, activityData] = await Promise.all([
                fetchDashboardStats(),
                fetchRecentActivity(),
            ])
            setStats((prev) => ({ ...prev, ...statsData }))
            setActivities(activityData)
            setLoading(false)
        }
        loadData()
    }, [])

    return (
        <DashboardLayout requiredRole="Agent">
            <div className="space-y-6">
                {/* Stats Grid */}
                <div className="grid gap-4 md:grid-cols-2">
                    <DashboardCard
                        title="Total Clients"
                        value={stats.totalClients}
                        description="Active client profiles"
                        icon={UserCheck}
                        color="text-blue-600"
                    />
                    <DashboardCard
                        title="Active Accounts"
                        value={stats.totalAccounts}
                        description="Client accounts managed"
                        icon={CreditCard}
                        color="text-green-600"
                    />
                </div>

                {/* Recent Activity */}
                <Card>
                    <CardHeader>
                        <CardTitle>Recent Activity</CardTitle>
                        <CardDescription>Latest client and account actions</CardDescription>
                    </CardHeader>
                    <CardContent>
                        {loading ? (
                            <p className="text-sm text-muted-foreground">Loading activity...</p>
                        ) : activities.length === 0 ? (
                            <p className="text-sm text-muted-foreground">No recent activity yet.</p>
                        ) : (
                            <div className="space-y-4">
                                {activities.map((activity, index) => (
                                    <div
                                        key={index}
                                        className="flex items-center justify-between py-2 border-b border-border last:border-0"
                                    >
                                        <div className="flex items-center gap-3">
                                            <div
                                                className={`w-2 h-2 rounded-full ${
                                                    activity.status === "completed" ? "bg-green-500" : "bg-yellow-500"
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

function DashboardCard({ title, value, description, icon: Icon, color }: any) {
    return (
        <Card className="hover:shadow-md transition-shadow">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">{title}</CardTitle>
                <Icon className={`h-4 w-4 ${color}`} />
            </CardHeader>
            <CardContent>
                <div className="text-2xl font-bold">{value}</div>
                <p className="text-xs text-muted-foreground">{description}</p>
            </CardContent>
        </Card>
    )
}
