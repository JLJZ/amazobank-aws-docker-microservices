"use client"

import { useCallback, useEffect, useState } from "react"
import { DashboardLayout } from "@/components/layout/dashboard-layout"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
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
} from "@/components/ui/alert-dialog"
import { UserPlus, Search, Edit, RotateCcw, Shield, Trash2 } from "lucide-react"
import { useToast } from "@/hooks/use-toast"
import {
  createUser as createUserRequest,
  deleteUser as deleteUserRequest,
  fetchUsers,
  updateUser as updateUserRequest,
  UserApiError,
} from "@/services/userApi"
import type { UserRole } from "@/types/roles"

interface User {
  userId: string
  firstName: string
  lastName: string
  email: string
  role: UserRole
  status?: "Active" | "Disabled"
  createdAt?: string
}

type ApiUser = {
  userId?: string
  id?: string
  firstName?: string
  lastName?: string
  email?: string
  role?: UserRole
  status?: "Active" | "Disabled"
  createdAt?: string
}

type ValidationErrors = Partial<Record<"firstName" | "lastName" | "email" | "password" | "general", string>>

const createFormDefaults = {
  firstName: "",
  lastName: "",
  email: "",
  role: "Agent" as UserRole,
}

const editFormDefaults = {
  ...createFormDefaults,
  password: "",
}

export default function UsersPage() {
  const [users, setUsers] = useState<User[]>([])
  const [searchTerm, setSearchTerm] = useState("")
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false)
  const [currentUserRole, setCurrentUserRole] = useState<UserRole>("Agent")
  const [isLoadingUsers, setIsLoadingUsers] = useState(true)
  const [fetchError, setFetchError] = useState<string | null>(null)
  const [createFormData, setCreateFormData] = useState(() => ({ ...createFormDefaults }))
  const [editFormData, setEditFormData] = useState(() => ({ ...editFormDefaults }))
  const [isUpdatingUser, setIsUpdatingUser] = useState(false)
  const [validationErrors, setValidationErrors] = useState<ValidationErrors>({})
  const [deleteTargetUser, setDeleteTargetUser] = useState<User | null>(null)
  const [deletingUserId, setDeletingUserId] = useState<string | null>(null)
  const { toast } = useToast()

  useEffect(() => {
    if (typeof window === "undefined") return
    try {
      const storedUser = localStorage.getItem("user")
      if (storedUser) {
        const parsed = JSON.parse(storedUser) as { Role?: UserRole }
        setCurrentUserRole(parsed.Role ?? "Agent")
      }
    } catch (error) {
      console.error("Failed to read current user role:", error)
    }
  }, [])

  const loadUsers = useCallback(
    async (options?: { quiet?: boolean }) => {
      const quiet = options?.quiet ?? false
      if (!quiet) {
        setIsLoadingUsers(true)
      }
      setFetchError(null)
      try {
        const response = await fetchUsers()
        const mappedUsers = (response as ApiUser[]).map((user) => ({
          userId: user.userId ?? user.id ?? `user-${Math.random().toString(36).slice(2, 10)}`,
          firstName: user.firstName ?? "",
          lastName: user.lastName ?? "",
          email: user.email ?? "",
          role: (user.role ?? "Agent") as UserRole,
          status: user.status ?? "Active",
          createdAt: user.createdAt,
        }))
        setUsers(mappedUsers)
      } catch (error) {
        console.error("Failed to fetch users:", error)
        const message = error instanceof Error ? error.message : "Unable to load users."
        setFetchError(message)
      } finally {
        if (!quiet) {
          setIsLoadingUsers(false)
        }
      }
    },
    [],
  )

  useEffect(() => {
    void loadUsers()
  }, [loadUsers])

  const filteredUsers = users.filter(
    (user) =>
      user.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.lastName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.userId.toLowerCase().includes(searchTerm.toLowerCase()),
  )

  const handleCreateUser = async () => {
    if (currentUserRole === "Admin" && createFormData.role === "Admin") {
      toast({
        variant: "destructive",
        title: "Action not allowed",
        description: "Admins are not allowed to create new Admin users.",
      })
      return
    }

    try {
      const createdUser = await createUserRequest({
        firstName: createFormData.firstName,
        lastName: createFormData.lastName,
        email: createFormData.email,
        role: createFormData.role,
      })

      const normalizedUser: User = {
        userId: createdUser?.userId || createdUser?.id || `user-${Math.random().toString(36).slice(2, 10)}`,
        firstName: createdUser?.firstName || createFormData.firstName,
        lastName: createdUser?.lastName || createFormData.lastName,
        email: createdUser?.email || createFormData.email,
        role: (createdUser?.role as UserRole) || createFormData.role,
        status: (createdUser as ApiUser)?.status ?? "Active",
        createdAt: (createdUser as ApiUser)?.createdAt || new Date().toISOString().split("T")[0],
      }

      setUsers((prev) => [...prev, normalizedUser])
      setCreateFormData({ ...createFormDefaults })
      setIsCreateDialogOpen(false)
      toast({
        title: "User created",
        description: `${normalizedUser.firstName} ${normalizedUser.lastName} has been added.`,
      })
      await loadUsers({ quiet: true })
    } catch (error) {
      console.error("Failed to create user:", error)
      const message = error instanceof Error ? error.message : "Failed to create user. Please try again."
      toast({
        variant: "destructive",
        title: "Unable to create user",
        description: message,
      })
    }
  }

  const openEditDialog = (user: User) => {
    setEditingUser(user)
    setEditFormData({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      role: user.role,
      password: "",
    })
    setValidationErrors({})
    setIsEditDialogOpen(true)
  }

  const closeEditDialog = () => {
    setIsEditDialogOpen(false)
    setEditingUser(null)
    setEditFormData({ ...editFormDefaults })
    setValidationErrors({})
  }

  const deriveValidationErrors = (message: string): ValidationErrors => {
    const normalized = message.toLowerCase()
    if (normalized.includes("first")) return { firstName: message }
    if (normalized.includes("last")) return { lastName: message }
    if (normalized.includes("email")) return { email: message }
    if (normalized.includes("password")) return { password: message }
    return { general: message }
  }

  const handleUpdateUser = async () => {
    if (!editingUser) return

    setIsUpdatingUser(true)
    setValidationErrors({})

    const payload = {
      firstName: editFormData.firstName,
      lastName: editFormData.lastName,
      email: editFormData.email,
      role: editFormData.role,
      ...(editFormData.password ? { password: editFormData.password } : {}),
    }

    try {
      await updateUserRequest(editingUser.userId, payload)
      setUsers((prev) =>
        prev.map((user) =>
          user.userId === editingUser.userId
            ? {
                ...user,
                firstName: payload.firstName,
                lastName: payload.lastName,
                email: payload.email,
                role: payload.role ?? user.role,
              }
            : user,
        ),
      )
      toast({
        title: "User updated",
        description: `${payload.firstName} ${payload.lastName} has been updated.`,
      })
      await loadUsers({ quiet: true })
      closeEditDialog()
    } catch (error) {
      console.error("Failed to update user:", error)
      const message = error instanceof Error ? error.message : "Failed to update user."
      if (error instanceof UserApiError && error.status === 422) {
        setValidationErrors(deriveValidationErrors(message))
      } else {
        setValidationErrors({})
      }
      toast({
        variant: "destructive",
        title: "Unable to update user",
        description: message,
      })
    } finally {
      setIsUpdatingUser(false)
    }
  }

  const handleResetPassword = (userId: string) => {
    console.log(`Password reset triggered for user ${userId}`)
  }

  const handleDeleteDialogOpenChange = (open: boolean) => {
    if (!open && !deletingUserId) {
      setDeleteTargetUser(null)
    }
  }

  const handleDeleteUser = async () => {
    if (!deleteTargetUser) return
    setDeletingUserId(deleteTargetUser.userId)
    try {
      await deleteUserRequest(deleteTargetUser.userId)
      setUsers((prev) => prev.filter((user) => user.userId !== deleteTargetUser.userId))
      toast({
        title: "User deleted",
        description: `${deleteTargetUser.firstName} ${deleteTargetUser.lastName} has been removed.`,
      })
      await loadUsers({ quiet: true })
      setDeleteTargetUser(null)
    } catch (error) {
      console.error("Failed to delete user:", error)
      const message = error instanceof Error ? error.message : "Failed to delete user."
      toast({
        variant: "destructive",
        title: "Unable to delete user",
        description: message,
      })
    } finally {
      setDeletingUserId(null)
    }
  }

  return (
    <DashboardLayout requiredRole="Admin">
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold text-balance">User Management</h2>
            <p className="text-muted-foreground">Create, update, and manage system users</p>
          </div>
          <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
            <DialogTrigger asChild>
              <Button>
                <UserPlus className="h-4 w-4 mr-2" />
                Create User
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Create New User</DialogTitle>
                <DialogDescription>Add a new user to the system</DialogDescription>
              </DialogHeader>
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="firstName">First Name</Label>
                    <Input
                      id="firstName"
                      value={createFormData.firstName}
                      onChange={(e) => setCreateFormData({ ...createFormData, firstName: e.target.value })}
                      placeholder="Enter first name"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="lastName">Last Name</Label>
                    <Input
                      id="lastName"
                      value={createFormData.lastName}
                      onChange={(e) => setCreateFormData({ ...createFormData, lastName: e.target.value })}
                      placeholder="Enter last name"
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="email">Email</Label>
                  <Input
                    id="email"
                    type="email"
                    value={createFormData.email}
                    onChange={(e) => setCreateFormData({ ...createFormData, email: e.target.value })}
                    placeholder="Enter email address"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="role">Role</Label>
                  <Select
                    value={createFormData.role}
                    onValueChange={(value: UserRole) => setCreateFormData({ ...createFormData, role: value })}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="Agent">Agent</SelectItem>
                      {currentUserRole === "SuperAdmin" && <SelectItem value="Admin">Admin</SelectItem>}
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsCreateDialogOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleCreateUser}>Create User</Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Search Users</CardTitle>
            <CardDescription>Find users by name, email, or ID</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="relative">
              <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search users..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Users ({filteredUsers.length})</CardTitle>
            <CardDescription>Manage system users and their permissions</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {isLoadingUsers ? (
                <p className="text-sm text-muted-foreground">Loading users...</p>
              ) : fetchError ? (
                <p className="text-sm text-destructive">{fetchError}</p>
              ) : filteredUsers.length === 0 ? (
                <p className="text-sm text-muted-foreground">No users found.</p>
              ) : (
                filteredUsers.map((user) => {
                  const isDeleting = deletingUserId === user.userId
                  return (
                    <div key={user.userId} className="flex items-center justify-between p-4 border border-border rounded-lg">
                      <div className="flex items-center gap-4">
                        <div className="w-10 h-10 bg-primary/10 rounded-full flex items-center justify-center">
                          <span className="text-sm font-medium text-primary">
                            {user.firstName[0]}
                            {user.lastName[0]}
                          </span>
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <h3 className="font-medium">
                              {user.firstName} {user.lastName}
                            </h3>
                            {user.role === "Admin" && <Shield className="h-4 w-4 text-primary" />}
                          </div>
                          <p className="text-sm text-muted-foreground">{user.email}</p>
                          <p className="text-xs text-muted-foreground">ID: {user.userId}</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        <Badge variant={(user.status ?? "Active") === "Active" ? "default" : "secondary"}>
                          {user.status ?? "Active"}
                        </Badge>
                        <Badge variant="outline">{user.role}</Badge>
                        <div className="flex items-center gap-1">
                          <Button variant="ghost" size="sm" onClick={() => openEditDialog(user)}>
                            <Edit className="h-4 w-4" />
                          </Button>
                          <Button variant="ghost" size="sm" onClick={() => handleResetPassword(user.userId)}>
                            <RotateCcw className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setDeleteTargetUser(user)}
                            disabled={isDeleting}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </div>
                    </div>
                  )
                })
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      <Dialog
        open={isEditDialogOpen}
        onOpenChange={(open) => {
          if (!open) {
            if (isUpdatingUser) return
            closeEditDialog()
          } else {
            setIsEditDialogOpen(true)
          }
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit User</DialogTitle>
            <DialogDescription>Update user information</DialogDescription>
          </DialogHeader>
          {validationErrors.general && <p className="text-sm text-destructive">{validationErrors.general}</p>}
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="editFirstName">First Name</Label>
                <Input
                  id="editFirstName"
                  value={editFormData.firstName}
                  disabled={isUpdatingUser}
                  onChange={(e) => setEditFormData({ ...editFormData, firstName: e.target.value })}
                />
                {validationErrors.firstName && <p className="text-sm text-destructive">{validationErrors.firstName}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="editLastName">Last Name</Label>
                <Input
                  id="editLastName"
                  value={editFormData.lastName}
                  disabled={isUpdatingUser}
                  onChange={(e) => setEditFormData({ ...editFormData, lastName: e.target.value })}
                />
                {validationErrors.lastName && <p className="text-sm text-destructive">{validationErrors.lastName}</p>}
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="editEmail">Email</Label>
              <Input
                id="editEmail"
                type="email"
                value={editFormData.email}
                disabled={isUpdatingUser}
                onChange={(e) => setEditFormData({ ...editFormData, email: e.target.value })}
              />
              {validationErrors.email && <p className="text-sm text-destructive">{validationErrors.email}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="editRole">Role</Label>
              <Select
                value={editFormData.role}
                disabled={isUpdatingUser}
                onValueChange={(value: UserRole) => setEditFormData({ ...editFormData, role: value })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="Agent">Agent</SelectItem>
                  {(currentUserRole === "SuperAdmin" || editFormData.role === "Admin") && (
                    <SelectItem value="Admin" disabled={currentUserRole !== "SuperAdmin"}>
                      Admin
                    </SelectItem>
                  )}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="editPassword">Password</Label>
              <Input
                id="editPassword"
                type="password"
                placeholder="Leave blank to keep current password"
                value={editFormData.password}
                disabled={isUpdatingUser}
                onChange={(e) => setEditFormData({ ...editFormData, password: e.target.value })}
              />
              {validationErrors.password && <p className="text-sm text-destructive">{validationErrors.password}</p>}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={closeEditDialog} disabled={isUpdatingUser}>
              Cancel
            </Button>
            <Button onClick={handleUpdateUser} disabled={isUpdatingUser || !editingUser}>
              {isUpdatingUser ? "Updating..." : "Update User"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog open={Boolean(deleteTargetUser)} onOpenChange={handleDeleteDialogOpenChange}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete user</AlertDialogTitle>
            <AlertDialogDescription>
              {deleteTargetUser
                ? `Are you sure you want to delete ${deleteTargetUser.firstName} ${deleteTargetUser.lastName}? This action cannot be undone.`
                : "Are you sure you want to delete this user? This action cannot be undone."}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={Boolean(deletingUserId)}>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleDeleteUser} disabled={Boolean(deletingUserId)}>
              {deletingUserId ? "Deleting..." : "Delete User"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </DashboardLayout>
  )
}
