let cachedBaseUrl: string | null = null

export function getApiBaseUrl(): string {
  if (cachedBaseUrl !== null) return cachedBaseUrl

  const raw = process.env.NEXT_PUBLIC_API_BASE_URL ?? ""
  cachedBaseUrl = raw.replace(/\/$/, "")

  if (!cachedBaseUrl && process.env.NODE_ENV === "development") {
    console.warn("NEXT_PUBLIC_API_BASE_URL is not set; API calls will use relative URLs.")
  }

  return cachedBaseUrl
}
