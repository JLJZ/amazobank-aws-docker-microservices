import { getAccessToken } from "@/services/authToken"
import { getApiBaseUrl } from "@/services/apiBase"

const API_BASE = `${getApiBaseUrl()}/api/clients`

function getAuthHeaders() {
  const token = getAccessToken()
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
}

async function handleResponse(res: Response) {
  let data: any = {}
  try {
    data = await res.json()
  } catch {
  }

  if (!res.ok) {
    throw new Error(data.message || `Request failed with ${res.status}`)
  }
  return data
}

// Fetch all clients
export async function fetchClients() {
  const res = await fetch(API_BASE, {
    method: "GET",
    headers: getAuthHeaders(),
  })
  return handleResponse(res)
}

// Fetch single client
export async function fetchClientById(clientId: string) {
  const res = await fetch(`${API_BASE}/${clientId}`, {
    method: "GET",
    headers: getAuthHeaders(),
  })
  return handleResponse(res)
}

// Create client
export async function createClient(clientData: any) {
  const res = await fetch(API_BASE, {
    method: "POST",
    headers: getAuthHeaders(),
    body: JSON.stringify(clientData),
  })
  return handleResponse(res)
}

// Update client
export async function updateClient(clientId: string, clientData: any) {
  const res = await fetch(`${API_BASE}/${clientId}`, {
    method: "PUT",
    headers: getAuthHeaders(),
    body: JSON.stringify(clientData),
  })
  return handleResponse(res)
}

// Verify client (WIP)
export async function verifyClient(clientId: string) {
  const res = await fetch(`${API_BASE}/${clientId}/verify`, {
    method: "POST",
    headers: getAuthHeaders(),
  })
  return handleResponse(res)
}

// Delete client
export async function deleteClient(clientId: string) {
  const res = await fetch(`${API_BASE}/${clientId}`, {
    method: "DELETE",
    headers: getAuthHeaders(),
  })
  return handleResponse(res)
}
