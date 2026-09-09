import { API_BASE_URL } from './config'
import type { ApiErrorResponse, ApiResponse } from './types'

const CORRELATION_ID_HEADER = 'X-Correlation-ID'

export class ApiClientError extends Error {
  readonly status: number
  readonly correlationId: string | null
  readonly response: ApiErrorResponse | null

  constructor(
    message: string,
    status: number,
    correlationId: string | null,
    response: ApiErrorResponse | null,
  ) {
    super(message)
    this.name = 'ApiClientError'
    this.status = status
    this.correlationId = correlationId
    this.response = response
  }
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (typeof value !== 'object' || value === null) return false

  const candidate = value as Record<string, unknown>
  return (
    typeof candidate.timestamp === 'string' &&
    typeof candidate.status === 'number' &&
    typeof candidate.error === 'string' &&
    Array.isArray(candidate.details) &&
    candidate.details.every((detail) => typeof detail === 'string')
  )
}

async function parseErrorBody(response: Response): Promise<ApiErrorResponse | null> {
  try {
    const body: unknown = await response.json()
    return isApiErrorResponse(body) ? body : null
  } catch {
    return null
  }
}

export async function apiRequest<T>(
  path: string,
  init?: RequestInit,
  responseType: 'json' | 'text' = 'json',
): Promise<ApiResponse<T>> {
  const headers = new Headers(init?.headers)
  if (!headers.has('Accept')) {
    headers.set('Accept', responseType === 'json' ? 'application/json' : 'text/plain')
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  })
  const correlationId = response.headers.get(CORRELATION_ID_HEADER)

  if (!response.ok) {
    const errorResponse = await parseErrorBody(response)
    const message = errorResponse?.error ?? `Request failed with status ${response.status}`
    throw new ApiClientError(message, response.status, correlationId, errorResponse)
  }

  const data = responseType === 'text' ? await response.text() : await response.json()
  return { data: data as T, correlationId }
}
