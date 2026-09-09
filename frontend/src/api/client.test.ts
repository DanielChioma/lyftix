import { API_BASE_URL } from './config'
import { ApiClientError, apiRequest, resetCsrfToken, setUnauthorizedHandler } from './client'

beforeEach(() => {
  resetCsrfToken()
  setUnauthorizedHandler(null)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

it('returns response data and captures the correlation id', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ value: 42 }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', 'X-Correlation-ID': 'request-42' },
      }),
    ),
  )

  await expect(apiRequest<{ value: number }>('/api/example')).resolves.toEqual({
    data: { value: 42 },
    correlationId: 'request-42',
  })
  expect(fetch).toHaveBeenCalledWith(`${API_BASE_URL}/api/example`, expect.any(Object))
  expect(fetch).toHaveBeenCalledWith(
    `${API_BASE_URL}/api/example`,
    expect.objectContaining({ credentials: 'include' }),
  )
})

it('adds a bootstrapped CSRF token to unsafe requests', async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'csrf-1' }))
    .mockResolvedValueOnce(jsonResponse({ created: true }, { status: 201 }))
  vi.stubGlobal('fetch', fetchMock)

  await apiRequest('/api/example', { method: 'POST' })

  expect(fetchMock).toHaveBeenNthCalledWith(1, `${API_BASE_URL}/api/auth/csrf`, expect.objectContaining({ credentials: 'include' }))
  const request = fetchMock.mock.calls[1][1] as RequestInit
  expect(new Headers(request.headers).get('X-XSRF-TOKEN')).toBe('csrf-1')
})

it('refreshes a stale CSRF token and retries an unsafe request once', async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'old' }))
    .mockResolvedValueOnce(new Response(null, { status: 403 }))
    .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'new' }))
    .mockResolvedValueOnce(jsonResponse({ updated: true }))
  vi.stubGlobal('fetch', fetchMock)

  await expect(apiRequest('/api/example', { method: 'POST' })).resolves.toMatchObject({ data: { updated: true } })
  expect(fetchMock).toHaveBeenCalledTimes(4)
  expect(new Headers((fetchMock.mock.calls[3][1] as RequestInit).headers).get('X-XSRF-TOKEN')).toBe('new')
})

it('does not retry login automatically after a 403', async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(jsonResponse({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'csrf-1' }))
    .mockResolvedValueOnce(new Response(null, { status: 403 }))
  vi.stubGlobal('fetch', fetchMock)

  await expect(apiRequest('/api/auth/login', { method: 'POST' })).rejects.toMatchObject({ status: 403 })
  expect(fetchMock).toHaveBeenCalledTimes(2)
})

it('notifies the session handler for 401 but not 403 responses', async () => {
  const handler = vi.fn()
  setUnauthorizedHandler(handler)
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 401 })))

  await expect(apiRequest('/api/example')).rejects.toMatchObject({ status: 401 })
  expect(handler).toHaveBeenCalledOnce()

  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 403 })))
  await expect(apiRequest('/api/example')).rejects.toMatchObject({ status: 403 })
  expect(handler).toHaveBeenCalledOnce()
})

function jsonResponse(body: unknown, init?: ResponseInit) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
    ...init,
  })
}

it('throws the typed backend error with details and correlation id', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          timestamp: '2026-09-09T10:00:00Z',
          status: 400,
          error: 'Validation Failed',
          details: ['name is required'],
        }),
        { status: 400, headers: { 'X-Correlation-ID': 'request-invalid' } },
      ),
    ),
  )

  const error = await apiRequest('/api/example').catch((reason: unknown) => reason)

  expect(error).toBeInstanceOf(ApiClientError)
  expect(error).toMatchObject({
    message: 'Validation Failed',
    status: 400,
    correlationId: 'request-invalid',
    response: { details: ['name is required'] },
  })
})

it('provides a useful fallback for a non-JSON error response', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('failure', { status: 502 })))

  await expect(apiRequest('/api/example')).rejects.toMatchObject({
    message: 'Request failed with status 502',
    status: 502,
    response: null,
  })
})
