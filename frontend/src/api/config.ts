const configuredBaseUrl = import.meta.env.VITE_LYFTIX_API_BASE_URL

export const API_BASE_URL = (
  configuredBaseUrl ?? (import.meta.env.DEV ? 'http://localhost:8080' : '')
).replace(/\/$/, '')
