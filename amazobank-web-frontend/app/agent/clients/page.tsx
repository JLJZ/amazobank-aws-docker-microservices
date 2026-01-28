"use client"

import { Suspense, useCallback, useEffect, useMemo, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { DashboardLayout } from "@/components/layout/dashboard-layout"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogTrigger,
} from "@/components/ui/dialog"
import {
  AlertDialog, AlertDialogContent, AlertDialogHeader, AlertDialogFooter, AlertDialogTitle,
  AlertDialogTrigger, AlertDialogAction, AlertDialogCancel,
} from "@/components/ui/alert-dialog"
import {
  Search, ArrowRight, UserPlus, Edit, Trash2, ArrowLeft, Plus, Phone, Mail, MapPin, CreditCard,
  DollarSign, Activity,
} from "lucide-react"
import {
  fetchClients, createClient, updateClient, deleteClient, fetchClientById, verifyClient,
} from "@/services/clientApi"
import { fetchAccounts, createAccount } from "@/services/accountApi"
import { fetchAccountTransactions } from "@/services/transactionApi"
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"


interface Client {
  clientId: string
  agentId: string
  firstName: string
  lastName: string
  dateOfBirth: string
  gender: string
  email: string
  phoneNumber: string
  address: string
  city: string
  state: string
  country: string
  postalCode: string
  verificationStatus?: "Verified" | "Unverified"
  clientStatus?: "Active" | "Deleted" | string
}

interface Account {
  accountId: string
  clientId: string
  accountType: "Savings" | "Checking" | "Business"
  accountStatus: "Active" | "Inactive" | "Pending"
  openingDate: string
  initialDeposit: number
  currency: string
  branchId: string
  currentBalance?: number
}

interface Transaction {
  transactionId: string
  clientId: string
  accountId: string
  transactionType: string
  amount: number
  date: string
  status: string
}

const normalizeTransactionType = (type?: string) =>
  typeof type === "string" ? type.trim().toUpperCase() : ""

const getTransactionDisplayData = (transactionType?: string) => {
  const normalizedType = normalizeTransactionType(transactionType)
  const isDeposit =
    normalizedType === "C" ||
    normalizedType === "CREDIT" ||
    normalizedType === "D" ||
    normalizedType === "DEPOSIT"

  const label =
    normalizedType === "D" || normalizedType === "DEPOSIT"
      ? "Deposit"
      : isDeposit
        ? "Credit"
        : normalizedType === "W" || normalizedType === "WITHDRAWAL"
          ? "Withdrawal"
          : "Transaction"

  return {
    isDeposit,
    label,
    iconClasses: isDeposit ? "bg-green-100 text-green-600" : "bg-red-100 text-red-600",
    amountClasses: isDeposit ? "text-green-600" : "text-red-600",
    amountPrefix: isDeposit ? "+" : "-",
  }
}

const isCompletedStatus = (status?: string) =>
  (status?.trim().toUpperCase() ?? "") === "COMPLETED"


function ClientsPageContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const clientId = searchParams.get("id")

  const [clients, setClients] = useState<Client[]>([])
  const [selectedClient, setSelectedClient] = useState<Client | null>(null)
  const [accounts, setAccounts] = useState<Account[]>([])
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [searchTerm, setSearchTerm] = useState("")
  const [loading, setLoading] = useState(true)
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [isAccountDialogOpen, setIsAccountDialogOpen] = useState(false)
  const [editingClient, setEditingClient] = useState<Client | null>(null)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [activeTab, setActiveTab] = useState("overview")
  const [verifyingClientId, setVerifyingClientId] = useState<string | null>(null)

  const [formData, setFormData] = useState<Omit<Client, "clientId" | "agentId">>({
    firstName: "",
    lastName: "",
    dateOfBirth: "",
    gender: "Male",
    email: "",
    phoneNumber: "",
    address: "",
    city: "Singapore",
    state: "Singapore",
    country: "Singapore",
    postalCode: "",
  })

  const [accountFormData, setAccountFormData] = useState({
    accountType: "Savings",
    initialDeposit: "",
    currency: "SGD",
    branchId: "",
  })

  // biome-ignore lint/correctness/useExhaustiveDependencies: <explanation>
  useEffect(() => {
    const loadClientDetails = async () => {
      if (clientId) {
        try {
          const clientRes = await fetchClientById(clientId)
          setSelectedClient(clientRes)
          const accRes = await fetchAccounts(clientId)
          const scopedAccounts = (accRes || []).filter(
            (account: Account) => account.clientId === clientId
          )
          setAccounts(scopedAccounts)
          if (accRes && accRes.length > 0) {
            const allTransactions = await Promise.all(
              scopedAccounts.map((acc: Account) => fetchAccountTransactions(acc.accountId).catch(() => []))
            )
            setTransactions(allTransactions.flat())
          } else {
            setTransactions([])
          }
        } catch (err) {
          console.error("Error loading client details:", err)
          router.push("/agent/clients")
        }
      } else {
        setSelectedClient(null)
        setAccounts([])
        setTransactions([])
      }
    }
    loadClientDetails()
  }, [clientId, router])

  const loadClients = useCallback(async () => {
    setLoading(true)
    try {
      const data = await fetchClients()
      setClients(data || [])
    } catch (err) {
      console.error("Error fetching clients:", err)
      setClients([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadClients()
  }, [loadClients])

  const resetForm = () => {
    setFormData({
      firstName: "",
      lastName: "",
      dateOfBirth: "",
      gender: "Male",
      email: "",
      phoneNumber: "",
      address: "",
      city: "Singapore",
      state: "Singapore",
      country: "Singapore",
      postalCode: "",
    })
    setEditingClient(null)
    setErrors({})
  }

  const handleFieldChange = (field: keyof typeof formData, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }))
    validateField(field, value)
  }

  const validateField = (field: keyof typeof formData, value: string) => {
    let error = ""

    switch (field) {
      case "firstName":
      case "lastName":
        if (!value.trim()) error = "Required"
        else if (value.length < 2) error = "Must be at least 2 characters"
        else if (value.length > 50) error = "Must not exceed 50 characters"
        else if (!/^[a-zA-Z\s]+$/.test(value)) error = "Letters and spaces only"
        break

      case "dateOfBirth":
        if (!value) error = "Required"
        else {
          const dob = new Date(value)
          const today = new Date()
          const age = today.getFullYear() - dob.getFullYear()
          if (dob >= today) error = "Must be in the past"
          else if (age < 18) error = "Client must be at least 18"
          else if (age > 100) error = "Client must be no more than 100"
        }
        break

      case "email":
        if (!value.trim()) error = "Required"
        else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) error = "Invalid email format"
        break

      case "phoneNumber":
        if (!value.trim()) error = "Required"
        else if (!/^\+\d{10,15}$/.test(value))
          error = "Format: +1234567890 (10–15 digits)"
        break

      case "address":
        if (!value.trim()) error = "Required"
        else if (value.length < 5) error = "Must be at least 5 characters"
        else if (value.length > 100) error = "Must not exceed 100 characters"
        break

      case "city":
      case "state":
      case "country":
        if (!value.trim()) error = "Required"
        else if (value.length < 2) error = "Must be at least 2 characters"
        else if (value.length > 50) error = "Must not exceed 50 characters"
        break

      case "postalCode":
        if (!value.trim()) error = "Required"
        else if (value.length < 4 || value.length > 10) error = "4–10 characters required"
        else if (formData.country === "Singapore" && !/^\d{6}$/.test(value)) {
          error = "Singapore postal code must be 6 digits"
        }
        break
    }

    setErrors(prev => ({ ...prev, [field]: error }))
    return error
  }


  const validateForm = () => {
    const newErrors: Record<string, string> = {}
    Object.entries(formData).forEach(([field, value]) => {
      const err = validateField(field as keyof typeof formData, value)
      if (err) newErrors[field] = err
    })
    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  // ✅ Save (create or update)
  const handleSave = async () => {
    if (!validateForm()) return
    try {
      if (editingClient) {
        const updated = await updateClient(editingClient.clientId, formData)
        setClients(prev =>
            prev.map(c => (c.clientId === editingClient.clientId ? updated : c))
        )
      } else {
        const created = await createClient(formData)
        setClients(prev => [...prev, created])
      }
      resetForm()
      setIsDialogOpen(false)
    } catch (err: any) {
      const msg = err.message || "Unexpected error"
      console.error("Error saving client:", msg)

      if (msg.toLowerCase().includes("email")) {
        setErrors(prev => ({ ...prev, email: msg }))
      } else if (msg.toLowerCase().includes("phone")) {
        setErrors(prev => ({ ...prev, phoneNumber: msg }))
      } else {
        alert(msg)
      }
    }
  }

  const handleDelete = async (id: string) => {
    try {
      await deleteClient(id)
      await loadClients()
    } catch (err) {
      console.error("Error deleting client:", err)
    }
  }

  const handleVerifyClient = async (id: string) => {
    setVerifyingClientId(id)
    try {
      await verifyClient(id)
      await loadClients()
    } catch (err) {
      console.error("Error verifying client:", err)
    } finally {
      setVerifyingClientId(null)
    }
  }

  const activeClients = clients.filter(client => client.clientStatus !== "Deleted")
  const filteredClients = activeClients.filter(
      c =>
          c.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
          c.lastName.toLowerCase().includes(searchTerm.toLowerCase())
  )
  const unverifiedClients = activeClients.filter(client => client.verificationStatus === "Unverified")
  const deletedClients = clients.filter(client => client.clientStatus === "Deleted")

  const calculateAge = (dob: string) => {
    if (!dob) return "-"
    const birthDate = new Date(dob)
    const today = new Date()
    let age = today.getFullYear() - birthDate.getFullYear()
    const m = today.getMonth() - birthDate.getMonth()
    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) age--
    return age
  }

  const getStatusColor = (status: Account["accountStatus"]) => {
    switch (status) {
      case "Active": return "default"
      case "Inactive": return "secondary"
      case "Pending": return "outline"
      default: return "secondary"
    }
  }

  const getTotalBalance = () => {
    return accounts.reduce((sum, acc) => sum + (acc.currentBalance ?? acc.initialDeposit), 0)
  }

  const handleCreateAccount = async () => {
    if (!clientId) return
    try {
      const newAccount = await createAccount({
        clientId,
        clientEmail: selectedClient?.email,
        accountType: accountFormData.accountType as "Savings" | "Checking" | "Business",
        initialDeposit: parseFloat(accountFormData.initialDeposit),
        currency: accountFormData.currency,
        branchId: accountFormData.branchId,
      })
      setAccounts((prev) => [...prev, newAccount])
      setAccountFormData({ accountType: "Savings", initialDeposit: "", currency: "SGD", branchId: "" })
      setIsAccountDialogOpen(false)
    } catch (err) {
      console.error("Error creating account:", err)
      alert("Failed to create account. Please try again.")
    }
  }

  if (selectedClient) {
    return (
        <DashboardLayout requiredRole="Agent">
          <div className="space-y-6">
            <Button variant="outline" size="sm" onClick={() => router.push("/agent/clients")}>
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Clients
            </Button>

            {/* Header */}
            <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg border p-6">
              <div className="flex items-start gap-4">
                <Avatar className="h-16 w-16">
                  <AvatarFallback className="text-lg font-semibold bg-blue-100 text-blue-700">
                    {selectedClient.firstName[0]}
                    {selectedClient.lastName[0]}
                  </AvatarFallback>
                </Avatar>
                <div className="space-y-3">
                  <h1 className="text-3xl font-bold">
                    {selectedClient.firstName} {selectedClient.lastName}
                  </h1>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-2 text-sm">
                    <span className="flex items-center gap-1"><Mail className="h-4 w-4" /> {selectedClient.email}</span>
                    <span className="flex items-center gap-1"><Phone className="h-4 w-4" /> {selectedClient.phoneNumber}</span>
                    <span><strong>Gender:</strong> {selectedClient.gender}</span>
                    <span><strong>Date of Birth:</strong> {selectedClient.dateOfBirth} (Age: {calculateAge(selectedClient.dateOfBirth)})</span>
                    <span className="flex items-center gap-1"><MapPin className="h-4 w-4" /> {selectedClient.address}, {selectedClient.city}, {selectedClient.state}, {selectedClient.country} {selectedClient.postalCode}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant="secondary">Client ID: {selectedClient.clientId}</Badge>
                  </div>
                </div>
              </div>
            </div>

            {/* Metrics */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Card>
                <CardContent className="p-4 flex justify-between">
                  <div>
                    <p className="text-sm text-muted-foreground">Total Balance</p>
                    <p className="text-2xl font-bold">SGD {getTotalBalance().toLocaleString()}</p>
                  </div>
                  <DollarSign className="h-8 w-8 text-green-600" />
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4 flex justify-between">
                  <div>
                    <p className="text-sm text-muted-foreground">Products Held</p>
                    <p className="text-2xl font-bold">{accounts.length}</p>
                  </div>
                  <CreditCard className="h-8 w-8 text-blue-600" />
                </CardContent>
              </Card>
            </div>

            {/* Tabs */}
            <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-4">
              <TabsList className="grid grid-cols-2">
                <TabsTrigger value="overview">Overview</TabsTrigger>
                <TabsTrigger value="accounts">Accounts</TabsTrigger>
              </TabsList>

              {/* Overview Tab */}
              <TabsContent value="overview" className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                  <CardHeader><CardTitle className="text-lg">Transactions</CardTitle></CardHeader>
                  <CardContent className="space-y-3">
                    {transactions.length === 0 ? (
                      <p className="text-muted-foreground">No transactions found.</p>
                    ) : (
                      transactions.map((tx) => {
                        const { label, iconClasses, amountClasses, amountPrefix } = getTransactionDisplayData(tx.transactionType)
                        const statusLabel = tx.status ?? "Unknown"
                        const statusVariant = isCompletedStatus(tx.status) ? "default" : "secondary"

                        return (
                          <div key={tx.transactionId} className="flex items-center justify-between p-2">
                            <div className="flex items-center gap-3">
                              <div className={`p-2 rounded-full ${iconClasses}`}>
                                <Activity className="h-3 w-3" />
                              </div>
                              <div>
                                <p className="text-sm font-medium">{label}</p>
                                <p className="text-xs text-muted-foreground">{new Date(tx.date).toLocaleDateString()}</p>
                                <p className="text-xs text-muted-foreground">Account: {tx.accountId}</p>
                              </div>
                            </div>
                            <div className="text-right">
                              <p className={`text-sm font-semibold ${amountClasses}`}>
                                {amountPrefix}SGD {tx.amount.toLocaleString()}
                              </p>
                              <Badge className="mt-1" variant={statusVariant}>{statusLabel}</Badge>
                            </div>
                          </div>
                        )
                      })
                    )}
                  </CardContent>
                </Card>
              </TabsContent>

              {/* Accounts Tab */}
              <TabsContent value="accounts">
                <Card>
                  <CardHeader className="flex flex-row items-center justify-between">
                    <div>
                      <CardTitle>Accounts ({accounts.length})</CardTitle>
                      <CardDescription>All client accounts</CardDescription>
                    </div>
                    <Dialog open={isAccountDialogOpen} onOpenChange={setIsAccountDialogOpen}>
                      <DialogTrigger asChild>
                        <Button size="sm"><Plus className="h-4 w-4 mr-2" /> Add Account</Button>
                      </DialogTrigger>
                      <DialogContent>
                        <DialogHeader><DialogTitle>Create New Account</DialogTitle></DialogHeader>
                        <div className="space-y-4">
                          <div>
                            <Label>Account Type</Label>
                            <Select value={accountFormData.accountType} onValueChange={(val) => setAccountFormData((prev) => ({ ...prev, accountType: val }))}>
                              <SelectTrigger><SelectValue placeholder="Select type" /></SelectTrigger>
                              <SelectContent>
                                <SelectItem value="Savings">Savings</SelectItem>
                                <SelectItem value="Checking">Checking</SelectItem>
                                <SelectItem value="Business">Business</SelectItem>
                              </SelectContent>
                            </Select>
                          </div>
                          <div>
                            <Label>Initial Deposit</Label>
                            <Input type="number" value={accountFormData.initialDeposit} onChange={(e) => setAccountFormData((prev) => ({ ...prev, initialDeposit: e.target.value }))} />
                          </div>
                          <div>
                            <Label>Currency</Label>
                            <Select value={accountFormData.currency} onValueChange={(val) => setAccountFormData((prev) => ({ ...prev, currency: val }))}>
                              <SelectTrigger><SelectValue placeholder="Select currency" /></SelectTrigger>
                              <SelectContent>
                                <SelectItem value="SGD">SGD</SelectItem>
                                <SelectItem value="USD">USD</SelectItem>
                                <SelectItem value="EUR">EUR</SelectItem>
                              </SelectContent>
                            </Select>
                          </div>
                          <div>
                            <Label>Branch ID</Label>
                            <Input value={accountFormData.branchId} onChange={(e) => setAccountFormData((prev) => ({ ...prev, branchId: e.target.value }))} />
                          </div>
                        </div>
                        <DialogFooter>
                          <Button variant="outline" onClick={() => setIsAccountDialogOpen(false)}>Cancel</Button>
                          <Button onClick={handleCreateAccount}>Create</Button>
                        </DialogFooter>
                      </DialogContent>
                    </Dialog>
                  </CardHeader>
                  <CardContent>
                    {accounts.length === 0 ? (
                        <p className="text-muted-foreground">No accounts yet.</p>
                    ) : (
                        accounts.map((acc) => (
                            <Card key={acc.accountId} className="mb-4 border-l-4 border-blue-500">
                              <CardContent className="p-6">
                                <div className="flex justify-between items-center mb-4">
                                  <div>
                                    <h3 className="text-lg font-semibold">{acc.accountType} Account</h3>
                                    <p className="text-sm text-muted-foreground">#{acc.accountId}</p>
                                  </div>
                                  <Badge variant={getStatusColor(acc.accountStatus)}>{acc.accountStatus}</Badge>
                                </div>
                                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                                  <div>
                                    <p className="text-sm text-muted-foreground">Balance</p>
                                    <p className="text-xl font-bold">{acc.currency} {(acc.currentBalance ?? acc.initialDeposit).toLocaleString()}</p>
                                  </div>
                                  <div>
                                    <p className="text-sm text-muted-foreground">Opened</p>
                                    <p className="font-medium">{new Date(acc.openingDate).toLocaleDateString()}</p>
                                  </div>
                                  <div>
                                    <p className="text-sm text-muted-foreground">Branch</p>
                                    <p className="font-medium">{acc.branchId}</p>
                                  </div>
                                  <div>
                                    <p className="text-sm text-muted-foreground">Initial Deposit</p>
                                    <p className="font-medium">{acc.currency} {acc.initialDeposit.toLocaleString()}</p>
                                  </div>
                                </div>
                              </CardContent>
                            </Card>
                        ))
                    )}
                  </CardContent>
                </Card>
              </TabsContent>
            </Tabs>
          </div>
        </DashboardLayout>
    )
  }

  return (
      <DashboardLayout requiredRole="Agent">
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-bold">Clients</h2>
            <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
              <DialogTrigger asChild>
                <Button onClick={resetForm}>
                  <UserPlus className="h-4 w-4 mr-2" /> Add Client
                </Button>
              </DialogTrigger>
              <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
                <DialogHeader>
                  <DialogTitle>{editingClient ? "Edit Client" : "Add Client"}</DialogTitle>
                </DialogHeader>
                <div className="grid grid-cols-2 gap-4">
                  {Object.keys(formData).map(field => (
                      <div key={field} className={field === "address" ? "col-span-2" : ""}>
                        <Label>{field.charAt(0).toUpperCase() + field.slice(1).replace(/([A-Z])/g, ' $1')}</Label>
                        {field === "gender" ? (
                            <Select
                                value={formData.gender}
                                onValueChange={(val) => handleFieldChange("gender", val)}
                            >
                              <SelectTrigger><SelectValue /></SelectTrigger>
                              <SelectContent>
                                <SelectItem value="Male">Male</SelectItem>
                                <SelectItem value="Female">Female</SelectItem>
                                <SelectItem value="Non-binary">Non-binary</SelectItem>
                                <SelectItem value="Prefer not to say">Prefer not to say</SelectItem>
                              </SelectContent>
                            </Select>
                        ) : (
                            <Input
                                type={field === "dateOfBirth" ? "date" : "text"}
                                value={(formData as any)[field]}
                                onChange={e => handleFieldChange(field as keyof typeof formData, e.target.value)}
                            />
                        )}
                        {errors[field] && <p className="text-xs text-red-500">{errors[field]}</p>}
                      </div>
                  ))}
                </div>
                <DialogFooter>
                  <Button variant="outline" onClick={() => setIsDialogOpen(false)}>Cancel</Button>
                  <Button onClick={handleSave}>{editingClient ? "Update" : "Create"}</Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </div>

          {/* Search */}
          <Card>
            <CardHeader>
              <CardTitle>Search Clients</CardTitle>
              <CardDescription>Find clients by name</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="relative">
                <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                <Input placeholder="Search clients..." value={searchTerm} onChange={e => setSearchTerm(e.target.value)} className="pl-10" />
              </div>
            </CardContent>
          </Card>

          {/* Unverified Clients */}
          <Card>
            <CardHeader>
              <CardTitle>Unverified Clients ({unverifiedClients.length})</CardTitle>
              <CardDescription>Clients awaiting verification</CardDescription>
            </CardHeader>
            <CardContent>
              {loading ? (
                <p className="text-muted-foreground">Loading...</p>
              ) : unverifiedClients.length === 0 ? (
                <p className="text-muted-foreground">All clients are verified.</p>
              ) : (
                <div className="space-y-4">
                  {unverifiedClients.map(client => (
                    <div
                      key={client.clientId}
                      className="flex flex-col gap-2 rounded-lg border p-4 md:flex-row md:items-center md:justify-between"
                    >
                      <div>
                        <h3 className="font-medium">{client.firstName} {client.lastName}</h3>
                        <p className="text-sm text-muted-foreground">{client.email}</p>
                        <p className="text-xs text-muted-foreground">{client.phoneNumber}</p>
                      </div>
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => handleVerifyClient(client.clientId)}
                        disabled={verifyingClientId === client.clientId}
                      >
                        {verifyingClientId === client.clientId ? "Verifying..." : "Verify"}
                      </Button>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Deleted Clients */}
          <Card>
            <CardHeader>
              <CardTitle>Deleted Clients ({deletedClients.length})</CardTitle>
              <CardDescription>Recently removed client records</CardDescription>
            </CardHeader>
            <CardContent>
              {loading ? (
                <p className="text-muted-foreground">Loading...</p>
              ) : deletedClients.length === 0 ? (
                <p className="text-muted-foreground">No deleted clients.</p>
              ) : (
                <div className="space-y-4">
                  {deletedClients.map(client => (
                    <div key={client.clientId} className="rounded-lg border p-4">
                      <h3 className="font-medium">{client.firstName} {client.lastName}</h3>
                      <p className="text-sm text-muted-foreground">{client.email}</p>
                      <p className="text-xs text-muted-foreground">Client ID: {client.clientId}</p>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Clients List */}
          <Card>
            <CardHeader><CardTitle>Clients ({filteredClients.length})</CardTitle></CardHeader>
            <CardContent>
              {loading ? (
                  <p className="text-muted-foreground">Loading...</p>
              ) : (
                  <div className="space-y-4">
                    {filteredClients.map(client => (
                        <div key={client.clientId} className="flex items-center justify-between p-4 border rounded-lg">
                          <div>
                            <h3 className="font-medium">{client.firstName} {client.lastName}</h3>
                            <p className="text-xs text-muted-foreground">{client.email} • {client.phoneNumber}</p>
                          </div>
                          <div className="flex gap-2">
                            <Button variant="ghost" size="sm" onClick={() => {
                              setEditingClient(client)
                              const { agentId, clientId, ...clientData } = client
                              setFormData(clientData)
                              setIsDialogOpen(true)
                            }}><Edit className="h-4 w-4" /></Button>
                            <AlertDialog>
                              <AlertDialogTrigger asChild><Button variant="ghost" size="sm"><Trash2 className="h-4 w-4" /></Button></AlertDialogTrigger>
                              <AlertDialogContent>
                                <AlertDialogHeader><AlertDialogTitle>Delete Client</AlertDialogTitle></AlertDialogHeader>
                                <p>Are you sure you want to delete {client.firstName} {client.lastName}?</p>
                                <AlertDialogFooter>
                                  <AlertDialogCancel>Cancel</AlertDialogCancel>
                                  <AlertDialogAction onClick={() => handleDelete(client.clientId)}>Delete</AlertDialogAction>
                                </AlertDialogFooter>
                              </AlertDialogContent>
                            </AlertDialog>
                            <Button variant="ghost" size="sm" onClick={() => router.push(`/agent/clients?id=${client.clientId}`)}><ArrowRight className="h-4 w-4" /></Button>
                          </div>
                        </div>
                    ))}
                    {filteredClients.length === 0 && !loading && <p className="text-muted-foreground">No clients found.</p>}
                  </div>
              )}
            </CardContent>
          </Card>
        </div>
      </DashboardLayout>
  )
}

export default function ClientsPage() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <ClientsPageContent />
    </Suspense>
  )
}
