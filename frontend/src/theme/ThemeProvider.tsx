import { type ReactNode, useCallback, useEffect, useLayoutEffect, useMemo, useState } from 'react'
import { ThemeContext } from './ThemeContext'
import { DARK_THEME_QUERY, parseThemePreference, THEME_STORAGE_KEY, type ResolvedTheme, type ThemePreference } from './theme'

function readPreference(): ThemePreference {
  try {
    return parseThemePreference(window.localStorage.getItem(THEME_STORAGE_KEY))
  } catch {
    return 'system'
  }
}

function readSystemTheme(): ResolvedTheme {
  return typeof window.matchMedia === 'function' && window.matchMedia(DARK_THEME_QUERY).matches ? 'dark' : 'light'
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [preference, setPreferenceState] = useState<ThemePreference>(readPreference)
  const [systemTheme, setSystemTheme] = useState<ResolvedTheme>(readSystemTheme)
  const resolvedTheme = preference === 'system' ? systemTheme : preference

  useLayoutEffect(() => {
    document.documentElement.dataset.theme = resolvedTheme
    document.documentElement.style.colorScheme = resolvedTheme
  }, [resolvedTheme])

  useEffect(() => {
    if (preference !== 'system' || typeof window.matchMedia !== 'function') return
    const mediaQuery = window.matchMedia(DARK_THEME_QUERY)
    const updateSystemTheme = (event: MediaQueryListEvent | MediaQueryList) => setSystemTheme(event.matches ? 'dark' : 'light')
    updateSystemTheme(mediaQuery)
    mediaQuery.addEventListener('change', updateSystemTheme)
    return () => mediaQuery.removeEventListener('change', updateSystemTheme)
  }, [preference])

  const setPreference = useCallback((nextPreference: ThemePreference) => {
    setPreferenceState(nextPreference)
    try {
      if (nextPreference === 'system') window.localStorage.removeItem(THEME_STORAGE_KEY)
      else window.localStorage.setItem(THEME_STORAGE_KEY, nextPreference)
    } catch {
      // A blocked storage API should not prevent in-session theme changes.
    }
  }, [])

  const value = useMemo(() => ({ preference, resolvedTheme, setPreference }), [preference, resolvedTheme, setPreference])
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}
