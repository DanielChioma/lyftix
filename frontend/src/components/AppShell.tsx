import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { BackendStatus } from './BackendStatus'
import { ThemeControl } from './ThemeControl'
import { useAuth } from '../auth/useAuth'
import { ApiErrorMessage } from './ApiErrorMessage'

const navigation = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/workouts', label: 'Workouts' },
  { to: '/productivity', label: 'Productivity' },
  { to: '/github', label: 'GitHub' },
  { to: '/coding', label: 'Coding' },
  { to: '/check-ins', label: 'Check-ins' },
  { to: '/system', label: 'System' },
]

export function AppShell() {
  const location = useLocation()
  const navigate = useNavigate()
  const auth = useAuth()
  const navigationRef = useRef<HTMLElement>(null)
  const [logoutError, setLogoutError] = useState<unknown>(null)
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  useEffect(() => {
    navigationRef.current
      ?.querySelector<HTMLAnchorElement>('a.active')
      ?.scrollIntoView?.({ block: 'nearest', inline: 'nearest' })
  }, [location.pathname])

  async function handleLogout() {
    setLogoutError(null)
    setIsLoggingOut(true)
    try {
      await auth.logout()
      navigate('/login', { replace: true })
    } catch (error) {
      setLogoutError(error)
    } finally {
      setIsLoggingOut(false)
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div>
          <NavLink className="brand" to="/" aria-label="Lyftix dashboard">
            <span className="brand-mark" aria-hidden="true">L</span>
            <span>Lyftix</span>
          </NavLink>
          <nav aria-label="Primary navigation" ref={navigationRef}>
            <ul>
              {navigation.map((item) => (
                <li key={item.to}>
                  <NavLink
                    className={({ isActive }) => (isActive ? 'active' : undefined)}
                    end={item.end}
                    to={item.to}
                  >
                    {item.label}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>
        </div>
        <div className="shell-controls">
          <div className="account-summary">
            <span>Signed in as</span>
            <strong>{auth.user?.username}</strong>
            <small>{auth.user?.role}</small>
            <button type="button" onClick={handleLogout} disabled={isLoggingOut}>
              {isLoggingOut ? 'Signing out…' : 'Sign out'}
            </button>
            {logoutError !== null && <ApiErrorMessage error={logoutError} />}
          </div>
          <ThemeControl />
          <BackendStatus />
        </div>
      </aside>
      <main className="content" id="main-content">
        <Outlet />
      </main>
    </div>
  )
}
