"use client"

import { useEffect, useState } from "react"; // Re-add useState import for error state
import { useAuth } from "react-oidc-context"; // Add useAuth import

import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Building2 } from "lucide-react";
import { useRouter } from "next/navigation";

export default function LoginPage() {
  const [error, setError] = useState("")
  const router = useRouter()

  const { signinRedirect, isLoading: authIsLoading, isAuthenticated } = useAuth(); // Get signinRedirect and loading state from useAuth
  
  useEffect(() => {
    if (isAuthenticated) {
      router.replace("/")
    }
  }, [isAuthenticated])

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")

    try {
      // Call the signinRedirect function from react-oidc-context
      // The actual redirection based on role will be handled by the onSignin callback in AuthProvider
      await signinRedirect(); // This will initiate the OIDC flow

    } catch (err: any) {
      console.error("Login error:", err)
      setError(err.message || "Something went wrong. Please try again.")
    }
  }

  return isAuthenticated ? <></> : (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="w-full max-w-md space-y-8">
        <div className="text-center">
          <div className="flex justify-center mb-4">
            <div className="p-3 bg-primary/10 rounded-full">
              <Building2 className="h-8 w-8 text-primary" />
            </div>
          </div>
          <h1 className="text-3xl font-bold text-balance">AmazoBank</h1>
          <p className="text-muted-foreground mt-2">Sign in to your account</p>
        </div>

        <Card className="border-border">
          <CardHeader>
            <CardTitle>Welcome back</CardTitle>
            <CardDescription>Enter your credentials to access the system</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleLogin} className="space-y-4">
              {error && (
                <Alert variant="destructive">
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}

              <Button type="submit" className="w-full" disabled={authIsLoading}> {/* Use authIsLoading */}
                {authIsLoading ? "Signing in..." : "Sign in"}
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
