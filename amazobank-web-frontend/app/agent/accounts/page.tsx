"use client"

import { useCallback, useEffect, useState } from "react"
import { DashboardLayout } from "@/components/layout/dashboard-layout"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import { CreditCard, Search, Trash2, DollarSign } from "lucide-react"

import { fetchAccounts, createAccount, deleteAccount } from "@/services/accountApi"
import { fetchClients, fetchClientById } from "@/services/clientApi"

interface Account {
  accountId: string
  clientId: string
  agentId: string
  accountType: "Savings" | "Checking" | "Business"
  accountStatus: "Active" | "Inactive" | "Pending" | "Deleted" | string
  openingDate: string
  initialDeposit: number
  currency: string
  branchId: string
  currentBalance?: number
}

interface Client {
  clientId: string
  firstName: string
  lastName: string
}

export default function AccountsPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [clients, setClients] = useState<Client[]>([])
  const [searchTerm, setSearchTerm] = useState("")
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)

  const [formData, setFormData] = useState({
    clientId: "",
    accountType: "Savings" as Account["accountType"],
    initialDeposit: "",
    branchId: "B001",
  })
  const [errors, setErrors] = useState<Record<string, string>>({})


  const loadData = useCallback(async () => {
    try {
      const accountsData = await fetchAccounts()
      setAccounts(accountsData || [])

      const clientsData = await fetchClients()
      setClients(clientsData || [])
    } catch (err) {
      console.error("Error loading data:", err)
    }
  }, [])

  useEffect(() => {
    void loadData()
  }, [loadData])

  const activeAccounts = accounts.filter((a) => a.accountStatus !== "Deleted")
  const deletedAccounts = accounts.filter((a) => a.accountStatus === "Deleted")

  const filteredAccounts = activeAccounts.filter(
      (a) =>
          a.accountId.toLowerCase().includes(searchTerm.toLowerCase()) ||
          a.clientId.toLowerCase().includes(searchTerm.toLowerCase()) ||
          a.accountType.toLowerCase().includes(searchTerm.toLowerCase())
  )
  const totalBalance = activeAccounts.reduce((sum, a) => sum + (a.currentBalance || a.initialDeposit), 0)

  const validateForm = () => {
    const newErrors: Record<string, string> = {}
    if (!formData.clientId) newErrors.clientId = "Please select a client"
    if (!formData.initialDeposit) newErrors.initialDeposit = "Initial deposit is required"

    const deposit = Number.parseFloat(formData.initialDeposit)
    if (isNaN(deposit) || deposit <= 0) {
      newErrors.initialDeposit = "Initial deposit must be a positive number"
    } else if (deposit < 100) {
      newErrors.initialDeposit = "Minimum initial deposit is $100"
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const resetForm = () => {
    setFormData({ clientId: "", accountType: "Savings", initialDeposit: "", branchId: "B001" })
    setErrors({})
  }

  const handleCreateAccount = async () => {
    if (!validateForm()) return
    try {
      const client = await fetchClientById(formData.clientId);
      const newAccount = await createAccount({
        clientId: formData.clientId,
        clientEmail: client.email,
        accountType: formData.accountType,
        initialDeposit: Number.parseFloat(formData.initialDeposit),
        branchId: formData.branchId,
        currency: "SGD",
      })
      setAccounts([...accounts, newAccount])
      resetForm()
      setIsCreateDialogOpen(false)
    } catch (err) {
      console.error("Error creating account:", err)
    }
  }

  const handleDeleteAccount = async (accountId: string) => {
    try {
      await deleteAccount(accountId)
      await loadData()
    } catch (err) {
      console.error("Error deleting account:", err)
    }
  }

  const getStatusColor = (status: Account["accountStatus"]) => {
    switch (status) {
      case "Active": return "default"
      case "Inactive": return "secondary"
      case "Pending": return "outline"
      default: return "secondary"
    }
  }

  return (
      <DashboardLayout requiredRole="Agent">
        <div className="space-y-6">
          {/* Header */}
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold">Account Management</h2>
              <p className="text-muted-foreground">Create and manage client accounts</p>
            </div>
            <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
              <DialogTrigger asChild>
                <Button onClick={resetForm}>
                  <CreditCard className="h-4 w-4 mr-2" />
                  Create Account
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Create New Account</DialogTitle>
                  <DialogDescription>Open a new account for an existing client</DialogDescription>
                </DialogHeader>
                <div className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="clientId">Select Client *</Label>
                    <Select
                        value={formData.clientId}
                        onValueChange={(value) => setFormData({ ...formData, clientId: value })}
                    >
                      <SelectTrigger className={errors.clientId ? "border-destructive" : ""}>
                        <SelectValue placeholder="Choose a client" />
                      </SelectTrigger>
                      <SelectContent>
                        {clients.map((client) => (
                            <SelectItem key={client.clientId} value={client.clientId}>
                              {client.firstName} {client.lastName} ({client.clientId})
                            </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    {errors.clientId && <p className="text-xs text-destructive">{errors.clientId}</p>}
                  </div>

                  <div className="space-y-2">
                    <Label>Account Type</Label>
                    <Select
                        value={formData.accountType}
                        onValueChange={(value: Account["accountType"]) => setFormData({ ...formData, accountType: value })}
                    >
                      <SelectTrigger><SelectValue /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="Savings">Savings</SelectItem>
                        <SelectItem value="Checking">Checking</SelectItem>
                        <SelectItem value="Business">Business</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="initialDeposit">Initial Deposit (SGD) *</Label>
                    <Input
                        id="initialDeposit"
                        type="number"
                        min="100"
                        value={formData.initialDeposit}
                        onChange={(e) => setFormData({ ...formData, initialDeposit: e.target.value })}
                        placeholder="Enter deposit amount"
                        className={errors.initialDeposit ? "border-destructive" : ""}
                    />
                    {errors.initialDeposit && <p className="text-xs text-destructive">{errors.initialDeposit}</p>}
                  </div>

                  <div className="space-y-2">
                    <Label>Branch</Label>
                    <Select
                        value={formData.branchId}
                        onValueChange={(value) => setFormData({ ...formData, branchId: value })}
                    >
                      <SelectTrigger><SelectValue /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="B001">Main Branch</SelectItem>
                        <SelectItem value="B002">Orchard Branch</SelectItem>
                        <SelectItem value="B003">Marina Branch</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                <DialogFooter>
                  <Button variant="outline" onClick={() => setIsCreateDialogOpen(false)}>Cancel</Button>
                  <Button onClick={handleCreateAccount}>Create</Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </div>

          {/* Stats */}
          <div className="grid gap-4 md:grid-cols-4">
            <Card>
              <CardHeader className="flex flex-row justify-between">
                <CardTitle className="text-sm font-medium">Total Accounts</CardTitle>
                <CreditCard className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{activeAccounts.length}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row justify-between">
                <CardTitle className="text-sm font-medium">Total Balance</CardTitle>
                <DollarSign className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  ${totalBalance.toLocaleString()}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Search */}
          <Card>
            <CardHeader>
              <CardTitle>Search Accounts</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="relative">
                <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                <Input
                    placeholder="Search accounts..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="pl-10"
                />
              </div>
            </CardContent>
          </Card>

          {/* Deleted Accounts */}
          <Card>
            <CardHeader>
              <CardTitle>Deleted Accounts ({deletedAccounts.length})</CardTitle>
            </CardHeader>
            <CardContent>
              {deletedAccounts.length === 0 ? (
                <p className="text-muted-foreground">No deleted accounts.</p>
              ) : (
                <div className="space-y-4">
                  {deletedAccounts.map((account) => (
                    <div key={account.accountId} className="rounded-lg border p-4">
                      <h3 className="font-medium">{account.accountType} Account</h3>
                      <p className="text-sm text-muted-foreground">Account ID: {account.accountId}</p>
                      <p className="text-xs text-muted-foreground">Client: {account.clientId}</p>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Accounts List */}
          <Card>
            <CardHeader>
              <CardTitle>Accounts ({filteredAccounts.length})</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {filteredAccounts.map((a) => (
                    <div key={a.accountId} className="flex items-center justify-between p-4 border rounded-lg">
                      <div>
                        <h3 className="font-medium">{a.accountType} Account</h3>
                        <Badge variant={getStatusColor(a.accountStatus)}>{a.accountStatus}</Badge>
                        <p className="text-sm">Client: {a.clientId}</p>
                        <p className="text-sm">Balance: ${a.currentBalance || a.initialDeposit} {a.currency}</p>
                      </div>
                      <AlertDialog>
                        <AlertDialogTrigger asChild>
                          <Button variant="ghost" size="sm">
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </AlertDialogTrigger>
                        <AlertDialogContent>
                          <AlertDialogHeader>
                            <AlertDialogTitle>Close Account</AlertDialogTitle>
                            <AlertDialogDescription>
                              Close account {a.accountId}? This cannot be undone.
                            </AlertDialogDescription>
                          </AlertDialogHeader>
                          <AlertDialogFooter>
                            <AlertDialogCancel>Cancel</AlertDialogCancel>
                            <AlertDialogAction onClick={() => handleDeleteAccount(a.accountId)}>
                              Confirm
                            </AlertDialogAction>
                          </AlertDialogFooter>
                        </AlertDialogContent>
                      </AlertDialog>
                    </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      </DashboardLayout>
  )
}
