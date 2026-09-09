import { Route, Routes } from 'react-router-dom'
import { AppShell } from './components/AppShell'
import { DashboardPage } from './pages/DashboardPage'
import { DomainPage } from './pages/DomainPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { WorkoutPage } from './pages/WorkoutPage'
import { ProductivityPage } from './pages/ProductivityPage'
import { GitHubPage } from './pages/GitHubPage'
import { CodingPage } from './pages/CodingPage'

export function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<DashboardPage />} />
        <Route path="workouts" element={<WorkoutPage />} />
        <Route path="productivity" element={<ProductivityPage />} />
        <Route path="github" element={<GitHubPage />} />
        <Route path="coding" element={<CodingPage />} />
        <Route path="check-ins" element={<DomainPage title="Daily check-ins" description="Connect mood, energy, sleep, and daily context." />} />
        <Route path="system" element={<DomainPage title="System metrics" description="Inspect the operational signals collected by Lyftix." />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
