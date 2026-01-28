"use client"

import React from "react";
import { AuthProvider as OidcAuthProvider } from "react-oidc-context"; // Import UserManagerSettings

const cognitoConfig = {
  authority: process.env.NEXT_PUBLIC_COGNITO_AUTHORITY,
  client_id: process.env.NEXT_PUBLIC_COGNITO_CLIENT_ID,
  redirect_uri: process.env.NEXT_PUBLIC_COGNITO_REDIRECT_URI,
  scope: process.env.NEXT_PUBLIC_COGNITO_SCOPE,
  post_logout_redirect_uri: process.env.NEXT_PUBLIC_COGNITO_REDIRECT_URI,

  // Callbacks should be within the config object
  onSignin: (oidcClient: any) => {
    console.error("Signed in successfully!")
    window.history.replaceState(
      {},
      document.title,
      window.location.pathname
    )
  },

  onSignout: () => {
    console.log("Signed out successfully!")
  },
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  return (
    <OidcAuthProvider {...cognitoConfig}>
      {children}
    </OidcAuthProvider>
  )
}
