import { type ReactNode, useCallback, useEffect, useMemo, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { getCurrentUser, login as loginRequest, logout as logoutRequest } from '../api/auth'
import type { AuthenticatedUser, LoginCredentials } from '../api/auth'
import { ApiClientError, resetCsrfToken, setUnauthorizedHandler } from '../api/client'
import { AuthContext, type AuthStatus } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [status, setStatus] = useState<AuthStatus>('checking')
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const [restorationError, setRestorationError] = useState<ApiClientError | Error | null>(null)

  const expireSession = useCallback(() => {
    setUser(null)
    setStatus('unauthenticated')
    setRestorationError(null)
    resetCsrfToken()
    queryClient.clear()
  }, [queryClient])

  useEffect(() => {
    setUnauthorizedHandler(expireSession)
    return () => setUnauthorizedHandler(null)
  }, [expireSession])

  const restoreSession = useCallback(async () => {
    setStatus('checking')
    setRestorationError(null)
    try {
      setUser(await getCurrentUser())
      setStatus('authenticated')
    } catch (error) {
      setUser(null)
      if (error instanceof ApiClientError && error.status === 401) {
        setStatus('unauthenticated')
      } else {
        setRestorationError(error instanceof Error ? error : new Error('Unable to verify your session'))
        setStatus('error')
      }
    }
  }, [])

  useEffect(() => {
    const restore = window.setTimeout(() => { void restoreSession() }, 0)
    return () => window.clearTimeout(restore)
  }, [restoreSession])

  const login = useCallback(async (credentials: LoginCredentials) => {
    const authenticatedUser = await loginRequest(credentials)
    setUser(authenticatedUser)
    setRestorationError(null)
    setStatus('authenticated')
  }, [])

  const logout = useCallback(async () => {
    await logoutRequest()
    expireSession()
  }, [expireSession])

  const value = useMemo(() => ({
    status, user, restorationError, login, logout, retryRestoration: restoreSession,
  }), [status, user, restorationError, login, logout, restoreSession])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
