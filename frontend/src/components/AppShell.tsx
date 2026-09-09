import { useEffect, useRef } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { BackendStatus } from './BackendStatus'
import { ThemeControl } from './ThemeControl'

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
  const navigationRef = useRef<HTMLElement>(null)

  useEffect(() => {
    navigationRef.current
      ?.querySelector<HTMLAnchorElement>('a.active')
      ?.scrollIntoView?.({ block: 'nearest', inline: 'nearest' })
  }, [location.pathname])

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
