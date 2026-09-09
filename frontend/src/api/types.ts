export interface ApiErrorResponse {
  timestamp: string
  status: number
  error: string
  details: string[]
}

export interface ApiResponse<T> {
  data: T
  correlationId: string | null
}
