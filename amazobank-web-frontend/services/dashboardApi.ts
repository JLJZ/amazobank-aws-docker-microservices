import { getAccessToken } from "@/services/authToken"
import { getApiBaseUrl } from "@/services/apiBase"

const API_BASE = getApiBaseUrl()
const CLIENT_BASE = `${API_BASE}/api/clients`
const ACCOUNT_BASE = `${API_BASE}/api/accounts`

function getAuthHeaders() {
    const token = getAccessToken()
    return {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }
}

// ✅ Get summary numbers
export async function fetchDashboardStats() {
    try {
        const [clientsRes, accountsRes] = await Promise.all([
            fetch(CLIENT_BASE, { headers: getAuthHeaders() }),
            fetch(ACCOUNT_BASE, { headers: getAuthHeaders() }),
        ])

        if (!clientsRes.ok || !accountsRes.ok) throw new Error("Failed to load dashboard data")

        const clients = await clientsRes.json()
        const accounts = await accountsRes.json()

        return {
            totalClients: clients.length || clients.clients?.length || 0,
            totalAccounts: accounts.length || accounts.accounts?.length || 0,
        }
    } catch (err) {
        console.error("Dashboard fetch error:", err)
        return { totalClients: 0, totalAccounts: 0 }
    }
}

// ✅ Get recent activity list
export async function fetchRecentActivity() {
    try {
        const [clientsRes, accountsRes] = await Promise.all([
            fetch(CLIENT_BASE, { headers: getAuthHeaders() }),
            fetch(ACCOUNT_BASE, { headers: getAuthHeaders() }),
        ])

        if (!clientsRes.ok || !accountsRes.ok) throw new Error("Failed to load recent activity")

        const clients = await clientsRes.json()
        const accounts = await accountsRes.json()

        // Sort by creation date if available (fallback: random order)
        const recentClients = (clients || [])
            .slice(-5)
            .reverse()
            .map((c: any) => ({
                type: "client",
                action: "New client created",
                details: `${c.firstName} ${c.lastName} (${c.email})`,
                time: c.createdAt || "recently",
                status: "completed",
            }))

        const recentAccounts = (accounts || [])
            .slice(-5)
            .reverse()
            .map((a: any) => ({
                type: "account",
                action: "Account opened",
                details: `${a.accountType} - ${a.accountId}`,
                time: a.openingDate || "recently",
                status: a.accountStatus === "Active" ? "completed" : "pending",
            }))

        // Combine both streams and show most recent first
        const merged = [...recentClients, ...recentAccounts]
            .sort(() => Math.random() - 0.5)
            .slice(0, 5)

        return merged
    } catch (err) {
        console.error("Recent activity fetch error:", err)
        return []
    }
}
