import { getCurrentUser, login, logout } from './auth'
import { API_BASE_URL } from './config'
import { resetCsrfToken } from './client'

beforeEach(() => resetCsrfToken())
afterEach(() => vi.unstubAllGlobals())

it('restores the current user with credentials', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ username: 'owner', role: 'OWNER' })))
  await expect(getCurrentUser()).resolves.toEqual({ username: 'owner', role: 'OWNER' })
  expect(fetch).toHaveBeenCalledWith(`${API_BASE_URL}/api/auth/me`, expect.objectContaining({ credentials: 'include' }))
})

it('logs in with CSRF protection and refreshes the token after authentication', async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(csrfResponse('anonymous-token'))
    .mockResolvedValueOnce(jsonResponse({ username: 'owner', role: 'OWNER' }))
    .mockResolvedValueOnce(csrfResponse('session-token'))
  vi.stubGlobal('fetch', fetchMock)

  await expect(login({ username: 'owner', password: 'secret' })).resolves.toEqual({ username: 'owner', role: 'OWNER' })
  expect(fetchMock).toHaveBeenCalledTimes(3)
  const loginInit = fetchMock.mock.calls[1][1] as RequestInit
  expect(new Headers(loginInit.headers).get('X-XSRF-TOKEN')).toBe('anonymous-token')
  expect(loginInit.body).toBe(JSON.stringify({ username: 'owner', password: 'secret' }))
})

it('logs out through a CSRF-protected request', async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(csrfResponse('session-token'))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
  vi.stubGlobal('fetch', fetchMock)

  await expect(logout()).resolves.toBeUndefined()
  const logoutInit = fetchMock.mock.calls[1][1] as RequestInit
  expect(new Headers(logoutInit.headers).get('X-XSRF-TOKEN')).toBe('session-token')
  expect(logoutInit.credentials).toBe('include')
})

function csrfResponse(token: string) {
  return jsonResponse({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token })
}

function jsonResponse(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
}
