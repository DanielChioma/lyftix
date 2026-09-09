import { ApiClientError } from '../api/client'

interface ApiErrorMessageProps {
  error: unknown
}

export function ApiErrorMessage({ error }: ApiErrorMessageProps) {
  if (error instanceof ApiClientError) {
    const details = error.response?.details ?? []
    return (
      <div className="api-error" role="alert">
        <p>{error.message}</p>
        {details.length > 0 && (
          <ul>
            {details.map((detail) => (
              <li key={detail}>{detail}</li>
            ))}
          </ul>
        )}
        {error.correlationId && (
          <p className="correlation-id">
            Request ID: <code>{error.correlationId}</code>
          </p>
        )}
      </div>
    )
  }

  return (
    <p className="api-error" role="alert">
      The service could not be reached. Check that the backend is running.
    </p>
  )
}
