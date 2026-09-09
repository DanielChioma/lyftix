import { NavLink, Outlet } from 'react-router-dom'
import { BackendStatus } from './BackendStatus'

const navigation = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/workouts', label: 'Workouts' },
  { to: '/github', label: 'GitHub' },
  { to: '/coding', label: 'Coding' },
  { to: '/check-ins', label: 'Check-ins' },
  { to: '/system', label: 'System' },
]

export function AppShell() {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div>
          <NavLink className="brand" to="/" aria-label="Lyftix dashboard">
            <span className="brand-mark" aria-hidden="true">L</span>
            <span>Lyftix</span>
          </NavLink>
          <nav aria-label="Primary navigation">
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
        <BackendStatus />
      </aside>
      <main className="content" id="main-content">
        <Outlet />
      </main>
    </div>
  )
}
