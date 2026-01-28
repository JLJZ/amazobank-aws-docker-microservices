"use client"

import { useState } from "react"
import { DashboardLayout } from "@/components/layout/dashboard-layout"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Loader2, Send, Sparkles } from "lucide-react"
import { generateEmailFromPrompt, sendGeneratedEmail } from "@/services/emailApi"
import { useToast } from "@/hooks/use-toast"

type FieldErrors = {
  recipient?: string
  subject?: string
  emailBody?: string
}

export default function AgentLlmEmailPage() {
  const [recipient, setRecipient] = useState("")
  const [subject, setSubject] = useState("")
  const [prompt, setPrompt] = useState("")
  const [emailBody, setEmailBody] = useState("")
  const [isGenerating, setIsGenerating] = useState(false)
  const [isSending, setIsSending] = useState(false)
  const [errors, setErrors] = useState<FieldErrors>({})
  const { toast } = useToast()

  const handleGenerate = async () => {
    if (!prompt.trim()) {
      toast({
        title: "Prompt required",
        description: "Describe what you want the LLM to write.",
        variant: "destructive",
      })
      return
    }

    setIsGenerating(true)
    try {
      const generated = await generateEmailFromPrompt(prompt.trim())
      setEmailBody(generated)
      toast({
        title: "Email generated",
        description: "Review or tweak the content before sending.",
      })
    } catch (error) {
      toast({
        title: "Unable to generate email",
        description: error instanceof Error ? error.message : "Please try again.",
        variant: "destructive",
      })
    } finally {
      setIsGenerating(false)
    }
  }

  const handleSend = async () => {
    const newErrors: FieldErrors = {}
    if (!recipient.trim()) newErrors.recipient = "Recipient is required"
    if (!subject.trim()) newErrors.subject = "Subject is required"
    if (!emailBody.trim()) newErrors.emailBody = "Email body is required"

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors)
      toast({
        title: "Missing details",
        description: "Recipient, subject, and email body are required to send.",
        variant: "destructive",
      })
      return
    }

    setErrors({})
    setIsSending(true)
    try {
      await sendGeneratedEmail({
        recipient: recipient.trim(),
        subject: subject.trim(),
        body: emailBody.trim(),
      })
      toast({
        title: "Email successfully sent",
        description: "All fields have been cleared for your next message.",
      })
      setRecipient("")
      setSubject("")
      setPrompt("")
      setEmailBody("")
      setErrors({})
    } catch (error) {
      toast({
        title: "Unable to send email",
        description: error instanceof Error ? error.message : "Please try again.",
        variant: "destructive",
      })
    } finally {
      setIsSending(false)
    }
  }

  return (
    <DashboardLayout requiredRole="Agent">
      <div className="max-w-4xl space-y-6">
        <div>
          <p className="text-sm font-semibold text-primary">LLM Email</p>
          <h1 className="text-3xl font-bold tracking-tight">Draft and send smarter emails</h1>
          <p className="text-muted-foreground">
            Use AmazoBank&apos;s LLM assistant to craft client-ready messaging, then edit and send directly from your
            workspace.
          </p>
        </div>

        <Card className="border-border/80 shadow-sm">
          <CardHeader className="space-y-1">
            <CardTitle>Compose with AI</CardTitle>
            <CardDescription>Provide a prompt to generate a tailored response, then personalize as needed.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-5">
            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="recipient">Recipient</Label>
                <Input
                  id="recipient"
                  value={recipient}
                  onChange={(event) => {
                    setRecipient(event.target.value)
                    if (errors.recipient) setErrors((prev) => ({ ...prev, recipient: undefined }))
                  }}
                  placeholder="client@amazobank.com"
                  required
                  aria-invalid={Boolean(errors.recipient)}
                  className={errors.recipient ? "border-destructive" : undefined}
                />
                {errors.recipient && <p className="text-xs text-destructive">{errors.recipient}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="subject">Subject</Label>
                <Input
                  id="subject"
                  value={subject}
                  onChange={(event) => {
                    setSubject(event.target.value)
                    if (errors.subject) setErrors((prev) => ({ ...prev, subject: undefined }))
                  }}
                  placeholder="Quarterly portfolio review"
                  required
                  aria-invalid={Boolean(errors.subject)}
                  className={errors.subject ? "border-destructive" : undefined}
                />
                {errors.subject && <p className="text-xs text-destructive">{errors.subject}</p>}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="prompt">Prompt</Label>
              <Textarea
                id="prompt"
                rows={5}
                value={prompt}
                onChange={(event) => setPrompt(event.target.value)}
                placeholder="Explain what the email should cover, tone, key bullet points..."
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="email-body">Email Body</Label>
              <Textarea
                id="email-body"
                rows={8}
                value={emailBody}
                onChange={(event) => {
                  setEmailBody(event.target.value)
                  if (errors.emailBody) setErrors((prev) => ({ ...prev, emailBody: undefined }))
                }}
                placeholder="The generated email will appear here for review."
                required
                aria-invalid={Boolean(errors.emailBody)}
                className={errors.emailBody ? "border-destructive" : undefined}
              />
              {errors.emailBody && <p className="text-xs text-destructive">{errors.emailBody}</p>}
            </div>

            <div className="flex flex-wrap gap-3 pt-2">
              <Button type="button" variant="outline" onClick={handleGenerate} disabled={isGenerating}>
                {isGenerating ? (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                ) : (
                  <Sparkles className="mr-2 h-4 w-4" />
                )}
                Generate Email
              </Button>
              <Button type="button" onClick={handleSend} disabled={isSending}>
                {isSending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Send className="mr-2 h-4 w-4" />}
                Send Email
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  )
}
