import { act, fireEvent, render, screen } from '@testing-library/react'
import { ThemeControl } from '../components/ThemeControl'
import { ThemeProvider } from './ThemeProvider'
import { DARK_THEME_QUERY, parseThemePreference, THEME_STORAGE_KEY } from './theme'
import { useTheme } from './useTheme'

function installMatchMedia(initialDark: boolean) {
  let matches = initialDark
  const listeners = new Set<(event: MediaQueryListEvent) => void>()
  const mediaQuery = {
    get matches() { return matches },
    media: DARK_THEME_QUERY,
    onchange: null,
    addEventListener: (_type: string, listener: (event: MediaQueryListEvent) => void) => { listeners.add(listener) },
    removeEventListener: (_type: string, listener: (event: MediaQueryListEvent) => void) => { listeners.delete(listener) },
    addListener: () => undefined,
    removeListener: () => undefined,
    dispatchEvent: () => true,
  } as unknown as MediaQueryList
  vi.stubGlobal('matchMedia', vi.fn(() => mediaQuery))

  return (dark: boolean) => {
    matches = dark
    const event = { matches: dark, media: DARK_THEME_QUERY } as MediaQueryListEvent
    listeners.forEach((listener) => listener(event))
  }
}

beforeEach(() => {
  const values = new Map<string, string>()
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => { values.set(key, value) },
    removeItem: (key: string) => { values.delete(key) },
    clear: () => { values.clear() },
    key: (index: number) => [...values.keys()][index] ?? null,
    get length() { return values.size },
  })
})

function ThemeState() {
  const { preference, resolvedTheme } = useTheme()
  return <output>{preference}:{resolvedTheme}</output>
}

function renderTheme() {
  return render(<ThemeProvider><ThemeControl /><ThemeState /></ThemeProvider>)
}

afterEach(() => {
  window.localStorage.clear()
  delete document.documentElement.dataset.theme
  document.documentElement.style.colorScheme = ''
  vi.unstubAllGlobals()
})

it('defaults to system and resolves a light system preference', () => {
  installMatchMedia(false)
  renderTheme()
  expect(screen.getByRole('combobox', { name: 'Theme' })).toHaveValue('system')
  expect(screen.getByText('system:light')).toBeInTheDocument()
  expect(document.documentElement).toHaveAttribute('data-theme', 'light')
})

it('resolves a dark system preference', () => {
  installMatchMedia(true)
  renderTheme()
  expect(screen.getByText('system:dark')).toBeInTheDocument()
  expect(document.documentElement).toHaveAttribute('data-theme', 'dark')
})

it.each(['light', 'dark'] as const)('restores a saved %s preference', (preference) => {
  installMatchMedia(preference === 'light')
  window.localStorage.setItem(THEME_STORAGE_KEY, preference)
  renderTheme()
  expect(screen.getByRole('combobox', { name: 'Theme' })).toHaveValue(preference)
  expect(screen.getByText(`${preference}:${preference}`)).toBeInTheDocument()
})

it('falls back safely for invalid stored preferences', () => {
  installMatchMedia(false)
  window.localStorage.setItem(THEME_STORAGE_KEY, 'sepia')
  renderTheme()
  expect(parseThemePreference('sepia')).toBe('system')
  expect(screen.getByText('system:light')).toBeInTheDocument()
})

it('continues with the system preference when storage reads are blocked', () => {
  installMatchMedia(true)
  vi.stubGlobal('localStorage', {
    getItem: () => { throw new Error('storage blocked') },
    setItem: () => undefined,
    removeItem: () => undefined,
    clear: () => undefined,
  })
  renderTheme()
  expect(screen.getByText('system:dark')).toBeInTheDocument()
})

it('tracks system theme changes while the system preference is selected', () => {
  const changeSystemTheme = installMatchMedia(false)
  renderTheme()
  act(() => changeSystemTheme(true))
  expect(screen.getByText('system:dark')).toBeInTheDocument()
  expect(document.documentElement).toHaveAttribute('data-theme', 'dark')
})

it('ignores later system changes when an explicit preference is selected', () => {
  const changeSystemTheme = installMatchMedia(false)
  renderTheme()
  fireEvent.change(screen.getByRole('combobox', { name: 'Theme' }), { target: { value: 'light' } })
  act(() => changeSystemTheme(true))
  expect(screen.getByText('light:light')).toBeInTheDocument()
  expect(document.documentElement).toHaveAttribute('data-theme', 'light')
})

it.each(['light', 'dark'] as const)('selecting %s persists the preference and updates the root theme', (preference) => {
  installMatchMedia(false)
  renderTheme()
  fireEvent.change(screen.getByRole('combobox', { name: 'Theme' }), { target: { value: preference } })
  expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe(preference)
  expect(document.documentElement).toHaveAttribute('data-theme', preference)
})

it('restores a selected preference after the provider remounts', () => {
  installMatchMedia(false)
  const view = renderTheme()
  fireEvent.change(screen.getByRole('combobox', { name: 'Theme' }), { target: { value: 'dark' } })
  view.unmount()
  renderTheme()
  expect(screen.getByRole('combobox', { name: 'Theme' })).toHaveValue('dark')
  expect(document.documentElement).toHaveAttribute('data-theme', 'dark')
})

it('selecting system clears persistence and resumes system resolution', () => {
  installMatchMedia(true)
  window.localStorage.setItem(THEME_STORAGE_KEY, 'light')
  renderTheme()
  fireEvent.change(screen.getByRole('combobox', { name: 'Theme' }), { target: { value: 'system' } })
  expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBeNull()
  expect(screen.getByText('system:dark')).toBeInTheDocument()
})
