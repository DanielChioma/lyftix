import type { ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { createQueryClient } from '../queryClient'

export function renderWithProviders(ui: ReactNode, route = '/') {
  const queryClient = createQueryClient()
  queryClient.setDefaultOptions({
    queries: { retry: false, staleTime: 0 },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
    </QueryClientProvider>,
  )
}
