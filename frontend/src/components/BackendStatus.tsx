import { useQuery } from '@tanstack/react-query'
import { ApiErrorMessage } from './ApiErrorMessage'
import { getBackendHealth } from '../api/health'

export function BackendStatus() {
  const healthQuery = useQuery({
    queryKey: ['backend-health'],
    queryFn: getBackendHealth,
    refetchInterval: 60_000,
    refetchIntervalInBackground: false,
  })

  if (healthQuery.isPending) {
    return <span className="status status-checking">Checking backend…</span>
  }

  if (healthQuery.isError) {
    return (
      <div className="status-panel">
        <span className="status status-unavailable">Backend unavailable</span>
        <ApiErrorMessage error={healthQuery.error} />
      </div>
    )
  }

  return <span className="status status-connected">Connected</span>
}
