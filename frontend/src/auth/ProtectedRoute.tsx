import type { ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { ApiErrorMessage } from '../components/ApiErrorMessage'
import { useAuth } from './useAuth'

export function ProtectedRoute() {
  const auth = useAuth()
  const location = useLocation()

  if (auth.status === 'checking') {
    return <SessionState title="Restoring your session" message="Checking your Lyftix account…" />
  }

  if (auth.status === 'error') {
    return (
      <SessionState title="We could not restore your session" message="Your sign-in status could not be confirmed.">
        <ApiErrorMessage error={auth.restorationError} />
        <button className="primary-button" type="button" onClick={auth.retryRestoration}>
          Try again
        </button>
      </SessionState>
    )
  }

  if (auth.status === 'unauthenticated') {
    return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />
  }

  return <Outlet />
}

interface SessionStateProps {
  title: string
  message: string
  children?: ReactNode
}

function SessionState({ title, message, children }: SessionStateProps) {
  return (
    <main className="session-page" id="main-content">
      <section className="session-card" aria-live="polite">
        <span className="brand-mark" aria-hidden="true">L</span>
        <p className="eyebrow">Lyftix</p>
        <h1>{title}</h1>
        <p className="lede">{message}</p>
        {children}
      </section>
    </main>
  )
}
