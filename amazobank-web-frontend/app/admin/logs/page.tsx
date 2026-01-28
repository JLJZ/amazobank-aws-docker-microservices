"use client"

import { useEffect, useMemo, useState, type ReactNode } from "react"
import { DashboardLayout } from "@/components/layout/dashboard-layout"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { fetchTransactionLogs } from "@/services/transactionApi"
import { Loader2 } from "lucide-react"

type LogSectionKey = "transaction_lambda" | "client_profile" | "client_account"

type RawLogEntry = {
  message: string
  logGroup: string
  id: string
  readableTimestamp: string
  logStream?: string
}

type ParsedLogEntry = {
  id: string
  level: string
  timestamp: string
  transactionId?: string
  clientId?: string
  accountId?: string
  logGroup: string
  logStream?: string
  message: string
}

type TransactionLogResponse = {
  total_scanned: number
  success_count: number
  logs?: Partial<Record<LogSectionKey, RawLogEntry[]>>
}

type LogColumn = {
  header: string
  className?: string
  render: (entry: ParsedLogEntry) => ReactNode
}

type LogSection = {
  key: LogSectionKey
  label: string
  description: string
  entries: ParsedLogEntry[]
}

const LOG_SECTION_METADATA: Array<Omit<LogSection, "entries">> = [
  {
    key: "transaction_lambda",
    label: "Transaction Logs",
    description: "AWS Lambda responsible for ingesting transaction CSV files.",
  },
  {
    key: "client_profile",
    label: "Client Profile Logs",
    description: "ECS service powering client lookup and profile management.",
  },
  {
    key: "client_account",
    label: "Client Account Logs",
    description: "ECS service that handles client accounts and transactions.",
  },
]

const TRANSACTION_COLUMNS: LogColumn[] = [
  {
    header: "Level",
    className: "py-3 px-2",
    render: (log) => <Badge variant="outline">{log.level}</Badge>,
  },
  {
    header: "Timestamp",
    className: "py-3 px-2 font-mono text-xs",
    render: (log) => log.timestamp,
  },
  {
    header: "Transaction ID",
    className: "py-3 px-2 font-mono text-xs break-all",
    render: (log) => log.transactionId ?? "—",
  },
  {
    header: "Client ID",
    className: "py-3 px-2 font-mono text-xs break-all",
    render: (log) => log.clientId ?? "—",
  },
  {
    header: "Account ID",
    className: "py-3 px-2 font-mono text-xs break-all",
    render: (log) => log.accountId ?? "—",
  },
  {
    header: "Message",
    className: "py-3 px-2 text-xs leading-relaxed whitespace-pre-wrap break-words",
    render: (log) => log.message,
  },
]

const DEFAULT_COLUMNS: LogColumn[] = [
  {
    header: "Level",
    className: "py-3 px-2",
    render: (log) => <Badge variant="outline">{log.level}</Badge>,
  },
  {
    header: "Timestamp",
    className: "py-3 px-2 font-mono text-xs",
    render: (log) => log.timestamp,
  },
  {
    header: "Message",
    className: "py-3 px-2 text-xs leading-relaxed whitespace-pre-wrap break-words",
    render: (log) => log.message,
  }
]

function parseField(message: string, key: string) {
  const match = message.match(new RegExp(`${key}=([^,\\s]+)`))
  return match ? match[1].replace(/[\r\n]+$/, "") : "—"
}

function parseLevel(message: string) {
  const match = message.match(/\[(.*?)\]/)
  return match ? match[1] : "INFO"
}

function parseLogEntry(entry: RawLogEntry): ParsedLogEntry {
  return {
    id: entry.id,
    level: parseLevel(entry.message),
    timestamp: entry.readableTimestamp || "—",
    logGroup: entry.logGroup,
    logStream: entry.logStream,
    message: entry.message.replace(/[\r\n]+$/, ""),
  }
}

function parseTransactionEntry(entry: RawLogEntry): ParsedLogEntry {
  const base = parseLogEntry(entry)
  return {
    ...base,
    transactionId: parseField(entry.message, "TransactionID"),
    clientId: parseField(entry.message, "ClientID"),
    accountId: parseField(entry.message, "AccountID"),
  }
}

export default function AdminLogsPage() {
  const [data, setData] = useState<TransactionLogResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [activeSection, setActiveSection] = useState<LogSectionKey>("transaction_lambda")

  useEffect(() => {
    const loadLogs = async () => {
      setIsLoading(true)
      setError(null)
      try {
        const response = await fetchTransactionLogs()
        setData(response as TransactionLogResponse)
      } catch (err) {
        const message = err instanceof Error ? err.message : "Failed to fetch logs."
        setError(message)
      } finally {
        setIsLoading(false)
      }
    }

    void loadLogs()
  }, [])

  const sections: LogSection[] = useMemo(() => {
    return LOG_SECTION_METADATA.map((section) => {
      const rawEntries = data?.logs?.[section.key] ?? []
      const parser = section.key === "transaction_lambda" ? parseTransactionEntry : parseLogEntry
      return {
        ...section,
        entries: rawEntries.map(parser),
      }
    })
  }, [data])

  const summary = {
    total: data?.total_scanned ?? 0,
    success: data?.success_count ?? 0,
  }
  const failure = Math.max(summary.total - summary.success, 0)

  return (
    <DashboardLayout requiredRole="Admin">
      <div className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>Logs Page</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="flex flex-col items-center justify-center py-16 text-muted-foreground">
                <Loader2 className="h-6 w-6 animate-spin mb-2" />
                <p>Loading logs...</p>
              </div>
            ) : error ? (
              <div className="rounded-md bg-destructive/10 border border-destructive text-destructive px-4 py-3">
                {error}
              </div>
            ) : <></>}
          </CardContent>
        </Card>
        {!isLoading && !error && (
          <Tabs
            value={activeSection}
            onValueChange={(value) => setActiveSection(value as LogSectionKey)}
            className="space-y-4"
          >
            <TabsList className="w-full flex flex-wrap gap-2">
              {sections.map((section) => (
                <TabsTrigger key={section.key} value={section.key} className="flex-1 min-w-[140px]">
                  {section.label}
                </TabsTrigger>
              ))}
            </TabsList>
            {sections.map((section) => {
              const columns = section.key === "transaction_lambda" ? TRANSACTION_COLUMNS : DEFAULT_COLUMNS
              return (
                <TabsContent key={section.key} value={section.key} className="space-y-4">
                  <Card>
                    <CardHeader>
                      <CardTitle>{section.label}</CardTitle>
                      <CardDescription>{section.description}</CardDescription>
                      <p className="text-sm text-muted-foreground">{section.entries.length} log entries</p>
                    </CardHeader>
                    <CardContent>
                      <div className="overflow-x-auto">
                        <table className="w-full text-sm">
                          <thead>
                            <tr className="text-left text-muted-foreground border-b border-border">
                              {columns.map((column) => (
                                <th key={column.header} className="py-3 px-2">
                                  {column.header}
                                </th>
                              ))}
                            </tr>
                          </thead>
                          <tbody>
                            {section.entries.length === 0 ? (
                              <tr>
                                <td colSpan={columns.length} className="py-6 text-center text-muted-foreground">
                                  No logs available for this section.
                                </td>
                              </tr>
                            ) : (
                              section.entries.map((log) => (
                                <tr key={`${section.key}-${log.id}`} className="border-b border-border/60 last:border-0">
                                  {columns.map((column) => (
                                    <td key={`${log.id}-${column.header}`} className={column.className ?? "py-3 px-2"}>
                                      {column.render(log)}
                                    </td>
                                  ))}
                                </tr>
                              ))
                            )}
                          </tbody>
                        </table>
                      </div>
                    </CardContent>
                  </Card>
                </TabsContent>
              )
            })}
          </Tabs>
        )}
      </div>
    </DashboardLayout>
  )
}
