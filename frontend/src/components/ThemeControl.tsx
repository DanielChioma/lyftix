import type { ThemePreference } from '../theme/theme'
import { useTheme } from '../theme/useTheme'

export function ThemeControl() {
  const { preference, setPreference } = useTheme()

  return (
    <label className="theme-control">
      <span>Theme</span>
      <select value={preference} onChange={(event) => setPreference(event.target.value as ThemePreference)}>
        <option value="system">System</option>
        <option value="light">Light</option>
        <option value="dark">Dark</option>
      </select>
    </label>
  )
}
