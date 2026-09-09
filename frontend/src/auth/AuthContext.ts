import { createContext } from 'react'
import type { ApiClientError } from '../api/client'
import type { AuthenticatedUser, LoginCredentials } from '../api/auth'

export type AuthStatus = 'checking' | 'authenticated' | 'unauthenticated' | 'error'

export interface AuthContextValue {
  status: AuthStatus
  user: AuthenticatedUser | null
  restorationError: ApiClientError | Error | null
  login: (credentials: LoginCredentials) => Promise<void>
  logout: () => Promise<void>
  retryRestoration: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
