import { getAccessToken } from "@/services/authToken"

const EMAIL_API_URL = process.env.NEXT_PUBLIC_API_BASE_URL + "/api/email"

interface SendEmailPayload {
  recipient: string
  subject: string
  body: string
}

function getHeaders() {
  const token = getAccessToken()
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
}

async function handleResponse(res: Response) {
  let data: any = null
  try {
    data = await res.json()
  } catch {
    // ignore JSON parse errors; API should respond with JSON but fall back to null
  }

  if (!res.ok) {
    const message = (data && (data.message || data.error)) || `Request failed with ${res.status}`
    throw new Error(message)
  }

  return data
}

export async function generateEmailFromPrompt(prompt: string) {
  if (!prompt.trim()) {
    throw new Error("Prompt cannot be empty.")
  }

  const res = await fetch(EMAIL_API_URL, {
    method: "POST",
    headers: getHeaders(),
    body: JSON.stringify({
      action: "generate",
      prompt,
    }),
  })

  const data = await handleResponse(res)

  if (!data || typeof data.body !== "string") {
    throw new Error("Email body is missing from the response.")
  }

  return data.body
}

export async function sendGeneratedEmail({ recipient, subject, body }: SendEmailPayload) {
  if (!recipient.trim() || !subject.trim() || !body.trim()) {
    throw new Error("Recipient, subject, and email body are required.")
  }

  const res = await fetch(EMAIL_API_URL, {
    method: "POST",
    headers: getHeaders(),
    body: JSON.stringify({
      action: "send",
      recipient,
      subject,
      body,
    }),
  })

  return handleResponse(res)
}
