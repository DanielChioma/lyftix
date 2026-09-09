import { screen } from '@testing-library/react'
import { BackendStatus } from './BackendStatus'
import { renderWithProviders } from '../test/renderApp'

afterEach(() => {
  vi.unstubAllGlobals()
})

it('shows a checking state while health is loading', () => {
  vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(() => undefined)))
  renderWithProviders(<BackendStatus />)
  expect(screen.getByText('Checking backend…')).toBeInTheDocument()
})

it('shows connected when the health endpoint succeeds', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('healthy', { status: 200 })))
  renderWithProviders(<BackendStatus />)
  expect(await screen.findByText('Backend connected')).toBeInTheDocument()
})

it('shows unavailable and the request id when health fails', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          timestamp: '2026-09-09T10:00:00Z',
          status: 503,
          error: 'Service Unavailable',
          details: ['Database unavailable'],
        }),
        { status: 503, headers: { 'X-Correlation-ID': 'request-123' } },
      ),
    ),
  )

  renderWithProviders(<BackendStatus />)

  expect(await screen.findByText('Backend unavailable')).toBeInTheDocument()
  expect(screen.getByText('Service Unavailable')).toBeInTheDocument()
  expect(screen.getByText('Database unavailable')).toBeInTheDocument()
  expect(screen.getByText('request-123')).toBeInTheDocument()
})
