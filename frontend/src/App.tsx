import { Route, Routes } from 'react-router-dom'
import { AppShell } from './components/AppShell'
import { DashboardPage } from './pages/DashboardPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { WorkoutPage } from './pages/WorkoutPage'
import { ProductivityPage } from './pages/ProductivityPage'
import { GitHubPage } from './pages/GitHubPage'
import { CodingPage } from './pages/CodingPage'
import { CheckInsPage } from './pages/CheckInsPage'
import { SystemMetricsPage } from './pages/SystemMetricsPage'
import { ThemeProvider } from './theme/ThemeProvider'
import { AuthProvider } from './auth/AuthProvider'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { LoginPage } from './pages/LoginPage'

export function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <Routes>
          <Route path="login" element={<LoginPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppShell />}>
              <Route index element={<DashboardPage />} />
              <Route path="workouts" element={<WorkoutPage />} />
              <Route path="productivity" element={<ProductivityPage />} />
              <Route path="github" element={<GitHubPage />} />
              <Route path="coding" element={<CodingPage />} />
              <Route path="check-ins" element={<CheckInsPage />} />
              <Route path="system" element={<SystemMetricsPage />} />
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Route>
        </Routes>
      </AuthProvider>
    </ThemeProvider>
  )
}
