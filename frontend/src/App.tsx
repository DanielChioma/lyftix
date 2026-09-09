import { Route, Routes } from 'react-router-dom'
import { AppShell } from './components/AppShell'
import { DashboardPage } from './pages/DashboardPage'
import { DomainPage } from './pages/DomainPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { WorkoutPage } from './pages/WorkoutPage'

export function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<DashboardPage />} />
        <Route path="workouts" element={<WorkoutPage />} />
        <Route path="github" element={<DomainPage title="GitHub activity" description="Review contributions and development momentum." />} />
        <Route path="coding" element={<DomainPage title="Coding sessions" description="Understand how focused development time accumulates." />} />
        <Route path="check-ins" element={<DomainPage title="Daily check-ins" description="Connect mood, energy, sleep, and daily context." />} />
        <Route path="system" element={<DomainPage title="System metrics" description="Inspect the operational signals collected by Lyftix." />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
