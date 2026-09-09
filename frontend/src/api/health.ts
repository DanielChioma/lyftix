import { apiRequest } from './client'

export function getBackendHealth() {
  return apiRequest<string>('/api/health', undefined, 'text')
}
