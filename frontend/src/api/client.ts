import { API_BASE_URL } from './config'
import type { ApiErrorResponse, ApiResponse } from './types'

const CORRELATION_ID_HEADER = 'X-Correlation-ID'
const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

interface CsrfTokenResponse {
  headerName: string
  parameterName: string
  token: string
}

export interface ApiRequestOptions extends RequestInit {
  handleUnauthorized?: boolean
  retryCsrf?: boolean
}

let csrfToken: Pick<CsrfTokenResponse, 'headerName' | 'token'> | null = null
let csrfRequest: Promise<Pick<CsrfTokenResponse, 'headerName' | 'token'>> | null = null
let unauthorizedHandler: (() => void) | null = null

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

export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler
}

export function resetCsrfToken() {
  csrfToken = null
  csrfRequest = null
}

export async function bootstrapCsrf() {
  if (csrfToken) return csrfToken
  if (!csrfRequest) {
    csrfRequest = apiRequest<CsrfTokenResponse>('/api/auth/csrf', { handleUnauthorized: false })
      .then(({ data }) => ({ headerName: data.headerName, token: data.token }))
      .catch((error) => {
        csrfRequest = null
        throw error
      })
  }
  csrfToken = await csrfRequest
  return csrfToken
}

export async function apiRequest<T>(
  path: string,
  init?: ApiRequestOptions,
  responseType: 'json' | 'text' = 'json',
): Promise<ApiResponse<T>> {
  const { handleUnauthorized = true, retryCsrf = path !== '/api/auth/login', ...requestInit } = init ?? {}
  const method = (requestInit.method ?? 'GET').toUpperCase()
  const headers = new Headers(init?.headers)
  if (!headers.has('Accept')) {
    headers.set('Accept', responseType === 'json' ? 'application/json' : 'text/plain')
  }

  if (UNSAFE_METHODS.has(method)) {
    const token = await bootstrapCsrf()
    headers.set(token.headerName, token.token)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...requestInit,
    headers,
    credentials: 'include',
  })
  const correlationId = response.headers.get(CORRELATION_ID_HEADER)

  if (!response.ok) {
    if (response.status === 403 && UNSAFE_METHODS.has(method) && retryCsrf) {
      resetCsrfToken()
      return apiRequest<T>(path, { ...requestInit, handleUnauthorized, retryCsrf: false }, responseType)
    }
    const errorResponse = await parseErrorBody(response)
    const message = errorResponse?.error ?? `Request failed with status ${response.status}`
    const error = new ApiClientError(message, response.status, correlationId, errorResponse)
    if (response.status === 401 && handleUnauthorized) unauthorizedHandler?.()
    throw error
  }

  const data = response.status === 204
    ? undefined
    : responseType === 'text' ? await response.text() : await response.json()
  return { data: data as T, correlationId }
}
