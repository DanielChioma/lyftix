import { apiRequest, bootstrapCsrf, resetCsrfToken } from './client'

export interface AuthenticatedUser {
  username: string
  role: string
}

export interface LoginCredentials {
  username: string
  password: string
}

export async function getCurrentUser() {
  return (await apiRequest<AuthenticatedUser>('/api/auth/me', { handleUnauthorized: false })).data
}

export async function login(credentials: LoginCredentials) {
  await bootstrapCsrf()
  const user = (await apiRequest<AuthenticatedUser>('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials),
    handleUnauthorized: false,
    retryCsrf: false,
  })).data
  resetCsrfToken()
  await bootstrapCsrf()
  return user
}

export async function logout() {
  await apiRequest<void>('/api/auth/logout', { method: 'POST' })
  resetCsrfToken()
}
