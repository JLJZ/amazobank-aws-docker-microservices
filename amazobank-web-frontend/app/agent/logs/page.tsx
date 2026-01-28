"use client"

import { useEffect, useMemo, useState } from "react"
import { DashboardLayout } from "@/components/layout/dashboard-layout"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { FileText, Search, AlertTriangle, AlertCircle, Info, Loader2 } from "lucide-react"
import { fetchTransactionLogs } from "@/services/transactionApi"

type RawLogEntry = {
  message: string
  logGroup: string
  id: string
  readableTimestamp: string
  logStream?: string
}

type TransactionLogResponse = {
  logs?: Partial<Record<"transaction_lambda" | "client_profile" | "client_account", RawLogEntry[]>>
}

type ParsedLogEntry = {
  id: string
  level: string
  timestamp: string
  transactionId?: string
  clientId?: string
  accountId?: string
  message: string
}

const normalizeLevel = (level?: string) => level?.toUpperCase() ?? "INFO"

const parseField = (message: string, key: string) => {
  const match = message.match(new RegExp(`${key}=([^,\\s]+)`))
  return match ? match[1].replace(/[\r\n]+$/, "") : ""
}

const parseLogEntry = (entry: RawLogEntry): ParsedLogEntry => ({
  id: entry.id,
  level: normalizeLevel(entry.message.match(/\[(.*?)\]/)?.[1]),
  timestamp: entry.readableTimestamp || "—",
  message: entry.message.replace(/[\r\n]+$/, ""),
})

const parseTransactionEntry = (entry: RawLogEntry): ParsedLogEntry => {
  const base = parseLogEntry(entry)
  return {
    ...base,
    transactionId: parseField(entry.message, "TransactionID") || undefined,
    clientId: parseField(entry.message, "ClientID") || undefined,
    accountId: parseField(entry.message, "AccountID") || undefined,
  }
}

const formatDateTime = (dateTime: string) => {
  if (!dateTime || dateTime === "—") return "—"
  const parsed = new Date(dateTime)
  return Number.isNaN(parsed.getTime()) ? dateTime : parsed.toLocaleString()
}

const getLevelBadgeVariant = (level: string) => {
  switch (normalizeLevel(level)) {
    case "ERROR":
      return "destructive"
    case "WARN":
    case "WARNING":
      return "secondary"
    default:
      return "outline"
  }
}

export default function LogsPage() {
  const [logs, setLogs] = useState<ParsedLogEntry[]>([])
  const [searchTerm, setSearchTerm] = useState("")
  const [levelFilter, setLevelFilter] = useState("all")
  const [clientFilter, setClientFilter] = useState("all")
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const loadLogs = async () => {
      setIsLoading(true)
      setError(null)
      try {
        const response = (await fetchTransactionLogs()) as TransactionLogResponse
        const rawEntries = response.logs?.transaction_lambda ?? []
        setLogs(rawEntries.map(parseTransactionEntry))
      } catch (err) {
        const message = err instanceof Error ? err.message : "Failed to fetch logs."
        setError(message)
        setLogs([])
      } finally {
        setIsLoading(false)
      }
    }

    void loadLogs()
  }, [])

  const filteredLogs = logs.filter((log) => {
    const normalizedLevel = normalizeLevel(log.level)
    const matchesLevel = levelFilter === "all" || normalizedLevel === levelFilter
    const matchesClient = clientFilter === "all" || log.clientId === clientFilter
    const term = searchTerm.toLowerCase()
    const matchesSearch =
      !term ||
      log.message.toLowerCase().includes(term) ||
      log.transactionId?.toLowerCase().includes(term) ||
      log.clientId?.toLowerCase().includes(term) ||
      log.accountId?.toLowerCase().includes(term)

    return matchesLevel && matchesClient && matchesSearch
  })

  const uniqueClients = useMemo(() => {
    const ids = new Set<string>()
    logs.forEach((log) => {
      if (log.clientId) ids.add(log.clientId)
    })
    return Array.from(ids)
  }, [logs])

  const totalLogs = logs.length
  const errorLogs = logs.filter((log) => normalizeLevel(log.level) === "ERROR").length
  const warningLogs = logs.filter((log) => normalizeLevel(log.level).startsWith("WARN")).length
  const infoLogs = logs.filter((log) => normalizeLevel(log.level) === "INFO").length

  return (
    <DashboardLayout requiredRole="Agent">
      <div className="space-y-6">
        {/* Header */}
        <div>
          <h2 className="text-2xl font-bold text-balance">Activity Logs</h2>
          <p className="text-muted-foreground">Track ingestion events from the transaction pipeline.</p>
        </div>

        {/* Stats */}
        <div className="grid gap-4 md:grid-cols-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Logs</CardTitle>
              <FileText className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{isLoading ? "…" : totalLogs}</div>
              <p className="text-xs text-muted-foreground">All recorded entries</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Errors</CardTitle>
              <AlertTriangle className="h-4 w-4 text-red-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-red-600">{isLoading ? "…" : errorLogs}</div>
              <p className="text-xs text-muted-foreground">Critical events</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Warnings</CardTitle>
              <AlertCircle className="h-4 w-4 text-yellow-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-yellow-600">{isLoading ? "…" : warningLogs}</div>
              <p className="text-xs text-muted-foreground">Needs review</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Info</CardTitle>
              <Info className="h-4 w-4 text-blue-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-blue-600">{isLoading ? "…" : infoLogs}</div>
              <p className="text-xs text-muted-foreground">Informational logs</p>
            </CardContent>
          </Card>
        </div>

        {/* Filters */}
        <Card>
          <CardHeader>
            <CardTitle>Filters</CardTitle>
            <CardDescription>Search by client, level, or keywords</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-3">
            <div className="flex items-center gap-2">
              <Search className="h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search message, client, transaction..."
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
              />
            </div>
            <Select value={levelFilter} onValueChange={setLevelFilter}>
              <SelectTrigger>
                <SelectValue placeholder="Log level" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Levels</SelectItem>
                <SelectItem value="ERROR">Errors</SelectItem>
                <SelectItem value="WARN">Warnings</SelectItem>
                <SelectItem value="INFO">Info</SelectItem>
              </SelectContent>
            </Select>
            <Select value={clientFilter} onValueChange={setClientFilter}>
              <SelectTrigger>
                <SelectValue placeholder="Client" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Clients</SelectItem>
                {uniqueClients.map((clientId) => (
                  <SelectItem key={clientId} value={clientId}>
                    {clientId}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </CardContent>
        </Card>

        {/* Logs */}
        <Card>
          <CardHeader>
            <CardTitle>Log Entries</CardTitle>
            <CardDescription>Latest records from transaction processing</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {isLoading ? (
              <div className="flex items-center text-sm text-muted-foreground">
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Loading logs...
              </div>
            ) : error ? (
              <p className="text-sm text-destructive">{error}</p>
            ) : filteredLogs.length === 0 ? (
              <p className="text-sm text-muted-foreground">No logs match the current filters.</p>
            ) : (
              filteredLogs.map((log) => (
                <div key={log.id} className="p-4 border border-border rounded-lg space-y-1">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <p className="text-sm font-semibold">
                        Transaction {log.transactionId ?? "entry"}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        Client: {log.clientId ?? "—"} • Account: {log.accountId ?? "—"}
                      </p>
                    </div>
                    <Badge variant={getLevelBadgeVariant(log.level)}>{normalizeLevel(log.level)}</Badge>
                  </div>
                  <p className="text-xs text-muted-foreground">{formatDateTime(log.timestamp)}</p>
                  <p className="text-sm text-foreground whitespace-pre-wrap break-words">{log.message}</p>
                </div>
              ))
            )}
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  )
}
