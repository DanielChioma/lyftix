import { fireEvent, screen, waitFor } from '@testing-library/react'
import { App } from './App'
import { renderWithProviders } from './test/renderApp'

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL) => {
    const url = String(input)
    if (url.endsWith('/api/auth/me')) {
      return Promise.resolve(jsonResponse({ username: 'owner', role: 'OWNER' }))
    }
    return new Promise<Response>(() => undefined)
  }))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('application routing', () => {
  it('renders the shell and primary navigation after session restoration', async () => {
    renderWithProviders(<App />)

    expect(await screen.findByRole('link', { name: 'Lyftix dashboard' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Primary navigation' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Productivity' })).toHaveAttribute('href', '/productivity')
    expect(screen.getByRole('heading', { name: 'Lyftix Overview' })).toBeInTheDocument()
  })

  it('brings the active navigation item into view for a deep link', async () => {
    const scrollIntoView = vi.fn()
    Element.prototype.scrollIntoView = scrollIntoView

    renderWithProviders(<App />, '/system')

    await waitFor(() => expect(scrollIntoView).toHaveBeenCalledWith({ block: 'nearest', inline: 'nearest' }))
    expect(screen.getByRole('link', { name: 'System' })).toHaveClass('active')
  })

  it.each([
    ['/workouts', 'Workout Analytics'],
    ['/productivity', 'Productivity'],
    ['/github', 'GitHub Activity'],
    ['/coding', 'Coding Sessions'],
    ['/check-ins', 'Daily Check-ins'],
    ['/system', 'System Metrics'],
  ])('routes %s to its page', async (route, heading) => {
    renderWithProviders(<App />, route)
    expect(await screen.findByRole('heading', { name: heading, level: 1 })).toBeInTheDocument()
  })

  it('renders the not-found route', async () => {
    renderWithProviders(<App />, '/missing')
    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Return to dashboard' })).toHaveAttribute('href', '/')
  })

  it('preserves the selected theme across route navigation', async () => {
    renderWithProviders(<App />)
    const theme = await screen.findByRole('combobox', { name: 'Theme' })
    fireEvent.change(theme, { target: { value: 'dark' } })
    fireEvent.click(screen.getByRole('link', { name: 'Workouts' }))
    expect(screen.getByRole('heading', { name: 'Workout Analytics' })).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Theme' })).toHaveValue('dark')
    expect(document.documentElement).toHaveAttribute('data-theme', 'dark')
  })

  it('redirects a protected deep link to login before mounting its page', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      if (String(input).endsWith('/api/auth/me')) return Promise.resolve(new Response(null, { status: 401 }))
      return new Promise<Response>(() => undefined)
    })

    renderWithProviders(<App />, '/workouts?range=30d')

    expect(await screen.findByRole('heading', { name: 'Welcome back' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Workout Analytics' })).not.toBeInTheDocument()
  })

  it('shows a recovery action instead of a false logged-out state when restoration fails', async () => {
    vi.mocked(fetch).mockRejectedValue(new TypeError('network unavailable'))
    renderWithProviders(<App />, '/workouts')

    expect(await screen.findByText('We could not restore your session')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Sign in' })).not.toBeInTheDocument()
  })

  it('shows a safe generic error for invalid credentials and clears the password', async () => {
    vi.mocked(fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/api/auth/me')) return Promise.resolve(new Response(null, { status: 401 }))
      if (url.endsWith('/api/auth/csrf')) return Promise.resolve(csrfResponse())
      if (url.endsWith('/api/auth/login') && init?.method === 'POST') {
        return Promise.resolve(new Response(null, { status: 401 }))
      }
      return new Promise<Response>(() => undefined)
    })
    renderWithProviders(<App />, '/login')

    fireEvent.change(await screen.findByRole('textbox', { name: 'Username' }), { target: { value: 'owner' } })
    const password = screen.getByLabelText('Password')
    fireEvent.change(password, { target: { value: 'wrong-password' } })
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Invalid username or password.')).toBeInTheDocument()
    expect(password).toHaveValue('')
  })

  it('returns to the intended protected deep link after login', async () => {
    vi.mocked(fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/api/auth/me')) return Promise.resolve(new Response(null, { status: 401 }))
      if (url.endsWith('/api/auth/csrf')) return Promise.resolve(csrfResponse())
      if (url.endsWith('/api/auth/login') && init?.method === 'POST') {
        return Promise.resolve(jsonResponse({ username: 'owner', role: 'OWNER' }))
      }
      return new Promise<Response>(() => undefined)
    })
    renderWithProviders(<App />, '/workouts?range=30d')

    fireEvent.change(await screen.findByRole('textbox', { name: 'Username' }), { target: { value: 'owner' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'correct-password' } })
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('heading', { name: 'Workout Analytics' })).toBeInTheDocument()
    expect(screen.getByText('owner')).toBeInTheDocument()
    expect(screen.getByText('OWNER')).toBeInTheDocument()
  })

  it('logs out and returns to the login page', async () => {
    vi.mocked(fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/api/auth/me')) return Promise.resolve(jsonResponse({ username: 'owner', role: 'OWNER' }))
      if (url.endsWith('/api/auth/csrf')) return Promise.resolve(csrfResponse())
      if (url.endsWith('/api/auth/logout') && init?.method === 'POST') return Promise.resolve(new Response(null, { status: 204 }))
      return new Promise<Response>(() => undefined)
    })
    renderWithProviders(<App />)

    fireEvent.click(await screen.findByRole('button', { name: 'Sign out' }))

    expect(await screen.findByRole('heading', { name: 'Welcome back' })).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Primary navigation' })).not.toBeInTheDocument()
  })
})

function jsonResponse(body: unknown, init?: ResponseInit) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
    ...init,
  })
}

function csrfResponse() {
  return jsonResponse({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'csrf-token' })
}
