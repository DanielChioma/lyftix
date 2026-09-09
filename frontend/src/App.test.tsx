import { screen } from '@testing-library/react'
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
    expect(screen.getByRole('heading', { name: 'Lyftix Overview' })).toBeInTheDocument()
  })

  it.each([
    ['/workouts', 'Workouts'],
    ['/github', 'GitHub activity'],
    ['/coding', 'Coding sessions'],
    ['/check-ins', 'Daily check-ins'],
    ['/system', 'System metrics'],
  ])('routes %s to its page', (route, heading) => {
    renderWithProviders(<App />, route)
    expect(screen.getByRole('heading', { name: heading, level: 1 })).toBeInTheDocument()
  })

  it('renders the not-found route', () => {
    renderWithProviders(<App />, '/missing')
    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Return to dashboard' })).toHaveAttribute('href', '/')
  })
})
