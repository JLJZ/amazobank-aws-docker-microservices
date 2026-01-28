export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null

  const storedIdToken = localStorage.getItem("idToken")
  if (storedIdToken) return storedIdToken

  const cachedToken = localStorage.getItem("token") ?? localStorage.getItem("accessToken")
  if (cachedToken) return cachedToken

  const authority = process.env.NEXT_PUBLIC_COGNITO_AUTHORITY
  const clientId = process.env.NEXT_PUBLIC_COGNITO_CLIENT_ID

  if (!authority || !clientId) return null

  const oidcStorageKey = `oidc.user:${authority}:${clientId}`
  const oidcUser =
    sessionStorage.getItem(oidcStorageKey) || localStorage.getItem(oidcStorageKey)

  if (!oidcUser) return null

  try {
    const parsed = JSON.parse(oidcUser)
    const idToken = parsed?.id_token ?? null
    const accessToken = parsed?.access_token ?? null

    if (idToken) {
      localStorage.setItem("idToken", idToken)
      localStorage.setItem("token", idToken)
      return idToken
    }

    if (accessToken) {
      localStorage.setItem("accessToken", accessToken)
      localStorage.setItem("token", accessToken)
      return accessToken
    }

    return null
  } catch (error) {
    console.error("Failed to parse OIDC session storage:", error)
    return null
  }
}
