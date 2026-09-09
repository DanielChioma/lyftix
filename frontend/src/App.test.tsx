import { fireEvent, screen } from '@testing-library/react'
import { App } from './App'
import { renderWithProviders } from './test/renderApp'

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(() => undefined)))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('application routing', () => {
  it('renders the shell and primary navigation', () => {
    renderWithProviders(<App />)

    expect(screen.getByRole('link', { name: 'Lyftix dashboard' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Primary navigation' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Productivity' })).toHaveAttribute('href', '/productivity')
    expect(screen.getByRole('heading', { name: 'Lyftix Overview' })).toBeInTheDocument()
  })

  it('brings the active navigation item into view for a deep link', () => {
    const scrollIntoView = vi.fn()
    Element.prototype.scrollIntoView = scrollIntoView

    renderWithProviders(<App />, '/system')

    expect(scrollIntoView).toHaveBeenCalledWith({ block: 'nearest', inline: 'nearest' })
    expect(screen.getByRole('link', { name: 'System' })).toHaveClass('active')
  })

  it.each([
    ['/workouts', 'Workout Analytics'],
    ['/productivity', 'Productivity'],
    ['/github', 'GitHub Activity'],
    ['/coding', 'Coding Sessions'],
    ['/check-ins', 'Daily Check-ins'],
    ['/system', 'System Metrics'],
  ])('routes %s to its page', (route, heading) => {
    renderWithProviders(<App />, route)
    expect(screen.getByRole('heading', { name: heading, level: 1 })).toBeInTheDocument()
  })

  it('renders the not-found route', () => {
    renderWithProviders(<App />, '/missing')
    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Return to dashboard' })).toHaveAttribute('href', '/')
  })

  it('preserves the selected theme across route navigation', () => {
    renderWithProviders(<App />)
    const theme = screen.getByRole('combobox', { name: 'Theme' })
    fireEvent.change(theme, { target: { value: 'dark' } })
    fireEvent.click(screen.getByRole('link', { name: 'Workouts' }))
    expect(screen.getByRole('heading', { name: 'Workout Analytics' })).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Theme' })).toHaveValue('dark')
    expect(document.documentElement).toHaveAttribute('data-theme', 'dark')
  })
})
