import { getAccessToken } from "@/services/authToken"
import { getApiBaseUrl } from "@/services/apiBase"

const API_BASE = getApiBaseUrl()

function getAuthHeaders() {
  const token = getAccessToken()
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
}

export async function fetchAccountTransactions(accountId: string) {
  const res = await fetch(`${API_BASE}/api/accounts/${accountId}/transactions`, {
    method: "GET",
    headers: getAuthHeaders(),
  })

  if (!res.ok) {
    throw new Error("Failed to fetch transactions")
  }

  return res.json()
}

export async function fetchTransactionLogs() {
  const res = await fetch(`${API_BASE}/api/transactions`, {
    method: "GET",
    headers: getAuthHeaders(),
  })

  if (!res.ok) {
    throw new Error("Failed to fetch transaction logs")
  }

  return res.json()
}
