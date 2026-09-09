import { API_BASE_URL } from './config'
import { ApiClientError, apiRequest } from './client'

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
})

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
