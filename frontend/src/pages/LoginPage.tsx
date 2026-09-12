import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiClientError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { ApiErrorMessage } from '../components/ApiErrorMessage'
import { BackendStatus } from '../components/BackendStatus'
import { getSafeDestination } from '../auth/redirect'

export function LoginPage() {
  const auth = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [submitError, setSubmitError] = useState<unknown>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const intendedDestination = getSafeDestination(
    (location.state as { from?: unknown } | null)?.from,
  )

  if (auth.status === 'authenticated') {
    return <Navigate to={intendedDestination} replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitError(null)
    setIsSubmitting(true)

    try {
      await auth.login({ username, password })
      navigate(intendedDestination, { replace: true })
    } catch (error) {
      setSubmitError(error)
    } finally {
      setPassword('')
      setIsSubmitting(false)
    }
  }

  return (
    <main className="login-page" id="main-content">
      <section className="login-card">
        <div className="login-brand">
          <span className="brand-mark" aria-hidden="true">L</span>
          <div>
            <p className="eyebrow">Lyftix</p>
            <h1>Welcome back</h1>
          </div>
        </div>
        <p className="lede">Sign in to view your personal analytics.</p>

        {auth.status === 'checking' && <p className="session-note" aria-live="polite">Checking your existing session…</p>}

        {auth.status === 'error' ? (
          <div className="login-recovery">
            <p>We could not confirm whether you are signed in.</p>
            <ApiErrorMessage error={auth.restorationError} />
            <button className="primary-button" type="button" onClick={auth.retryRestoration}>Try again</button>
          </div>
        ) : (
          <form className="login-form" onSubmit={handleSubmit}>
            <label>
              <span>Username</span>
              <input
                autoComplete="username"
                name="username"
                required
                value={username}
                onChange={(event) => setUsername(event.target.value)}
              />
            </label>
            <label>
              <span>Password</span>
              <input
                autoComplete="current-password"
                name="password"
                required
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </label>
            {submitError instanceof ApiClientError && submitError.status === 401 ? (
              <p className="api-error" role="alert">Invalid username or password.</p>
            ) : submitError ? (
              <ApiErrorMessage error={submitError} />
            ) : null}
            <button className="primary-button" disabled={isSubmitting || auth.status === 'checking'} type="submit">
              {isSubmitting ? 'Signing in…' : 'Sign in'}
            </button>
          </form>
        )}

        <div className="login-controls">
          {/*<ThemeControl />*/}
          <BackendStatus />
        </div>
      </section>
    </main>
  )
}
