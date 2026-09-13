import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ApiClientError } from '../api/client'
import { createDailyCheckIn } from '../api/checkIns'
import { CreateDailyCheckInForm } from './CreateDailyCheckInForm'

vi.mock('../api/checkIns', () => ({ createDailyCheckIn: vi.fn() }))

const response = {
  id: 1, checkInDate: '2026-09-13', mood: 8, energy: 7, focus: 9, stress: 3,
  productivity: 8, sleepMinutes: 465, notes: null,
  createdAt: '2026-09-13T20:00:00Z', updatedAt: '2026-09-13T20:00:00Z',
}

function renderForm() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const invalidate = vi.spyOn(client, 'invalidateQueries').mockResolvedValue()
  const onClose = vi.fn()
  const onCreated = vi.fn()
  const view = render(<QueryClientProvider client={client}><CreateDailyCheckInForm onClose={onClose} onCreated={onCreated} /></QueryClientProvider>)
  return { invalidate, onClose, onCreated, unmount: view.unmount }
}

function fillValidForm(notes = '') {
  fireEvent.change(screen.getByLabelText('Check-in date'), { target: { value: '2026-09-13' } })
  fireEvent.change(screen.getByLabelText(/^Mood/), { target: { value: '8' } })
  fireEvent.change(screen.getByLabelText(/^Energy/), { target: { value: '7' } })
  fireEvent.change(screen.getByLabelText(/^Focus/), { target: { value: '9' } })
  fireEvent.change(screen.getByLabelText(/^Stress/), { target: { value: '3' } })
  fireEvent.change(screen.getByLabelText(/^Productivity/), { target: { value: '8' } })
  fireEvent.change(screen.getByLabelText(/^Sleep hours/), { target: { value: '7' } })
  fireEvent.change(screen.getByLabelText(/^Sleep minutes/), { target: { value: '45' } })
  fireEvent.change(screen.getByLabelText(/^Notes/), { target: { value: notes } })
}

beforeEach(() => { vi.mocked(createDailyCheckIn).mockReset() })

it('submits the exact payload, preserves LocalDate, invalidates dependencies, and closes', async () => {
  vi.mocked(createDailyCheckIn).mockResolvedValue(response)
  const { invalidate, onClose, onCreated } = renderForm()
  fillValidForm()
  fireEvent.click(screen.getByRole('button', { name: 'Save check-in' }))
  await waitFor(() => expect(createDailyCheckIn).toHaveBeenCalledWith({
    checkInDate: '2026-09-13', mood: 8, energy: 7, focus: 9, stress: 3,
    productivity: 8, sleepMinutes: 465, notes: null,
  }))
  await waitFor(() => expect(onClose).toHaveBeenCalledOnce())
  expect(onCreated).toHaveBeenCalledOnce()
  expect(invalidate).toHaveBeenCalledWith({ queryKey: ['check-ins'] })
  expect(invalidate).toHaveBeenCalledWith({ queryKey: ['analytics', 'check-ins'] })
  expect(invalidate).toHaveBeenCalledWith({ queryKey: ['analytics', 'daily-summary'] })
})

it('preserves nonblank optional notes', async () => {
  vi.mocked(createDailyCheckIn).mockResolvedValue({ ...response, notes: 'Good focus.' })
  renderForm(); fillValidForm('Good focus.')
  fireEvent.click(screen.getByRole('button', { name: 'Save check-in' }))
  await waitFor(() => expect(createDailyCheckIn).toHaveBeenCalledWith(expect.objectContaining({ notes: 'Good focus.' })))
})

it('validates every required field without submitting', async () => {
  renderForm()
  fireEvent.change(screen.getByLabelText('Check-in date'), { target: { value: '' } })
  fireEvent.click(screen.getByRole('button', { name: 'Save check-in' }))
  expect(await screen.findByText('Choose a check-in date.')).toBeInTheDocument()
  expect(screen.getByText('Enter mood from 1 to 10.')).toBeInTheDocument()
  expect(screen.getByText('Enter energy from 1 to 10.')).toBeInTheDocument()
  expect(screen.getByText('Enter focus from 1 to 10.')).toBeInTheDocument()
  expect(screen.getByText('Enter stress from 1 to 10.')).toBeInTheDocument()
  expect(screen.getByText('Enter productivity from 1 to 10.')).toBeInTheDocument()
  expect(screen.getByText('Enter sleep hours.')).toBeInTheDocument()
  expect(screen.getByText('Enter the remaining sleep minutes.')).toBeInTheDocument()
  expect(createDailyCheckIn).not.toHaveBeenCalled()
})

it.each(['0', '11', '2.5'])('rejects invalid subjective rating %s', async (value) => {
  renderForm(); fillValidForm()
  fireEvent.change(screen.getByLabelText(/^Mood/), { target: { value } })
  fireEvent.click(screen.getByRole('button', { name: 'Save check-in' }))
  expect(await screen.findByText('Mood must be a whole number from 1 to 10.')).toBeInTheDocument()
  expect(createDailyCheckIn).not.toHaveBeenCalled()
})

it.each(['60', '61'])('rejects sleep minutes outside the 0 to 59 range: %s', async (value) => {
  renderForm(); fillValidForm()
  fireEvent.change(screen.getByLabelText(/^Sleep minutes/), { target: { value } })
  fireEvent.click(screen.getByRole('button', { name: 'Save check-in' }))
  expect(await screen.findByText('Sleep minutes must be a whole number from 0 to 59.')).toBeInTheDocument()
  expect(createDailyCheckIn).not.toHaveBeenCalled()
})

it.each([
  ['8', '0', 480],
  ['8', '30', 510],
  ['0', '59', 59],
])('converts %sh %sm to exactly %s sleep minutes', async (hours, minutes, totalMinutes) => {
  vi.mocked(createDailyCheckIn).mockResolvedValue(response)
  renderForm(); fillValidForm()
  fireEvent.change(screen.getByLabelText(/^Sleep hours/), { target: { value: hours } })
  fireEvent.change(screen.getByLabelText(/^Sleep minutes/), { target: { value: minutes } })
  fireEvent.click(screen.getByRole('button', { name: 'Save check-in' }))
  await waitFor(() => expect(createDailyCheckIn).toHaveBeenCalledWith(expect.objectContaining({ sleepMinutes: totalMinutes })))
})

it.each([
  ['Sleep hours', '-1', 'Sleep hours must be a non-negative whole number.'],
  ['Sleep minutes', '-1', 'Sleep minutes must be a whole number from 0 to 59.'],
])('rejects a negative %s value', async (label, value, message) => {
  renderForm(); fillValidForm()
  fireEvent.change(screen.getByLabelText(new RegExp(`^${label}`)), { target: { value } })
  fireEvent.click(screen.getByRole('button', { name: 'Save check-in' }))
  expect(await screen.findByText(message)).toBeInTheDocument()
  expect(createDailyCheckIn).not.toHaveBeenCalled()
})

it.each([
  ['Sleep hours', '7.5', 'Sleep hours must be a non-negative whole number.'],
  ['Sleep minutes', '30.5', 'Sleep minutes must be a whole number from 0 to 59.'],
])('rejects a decimal %s value', async (label, value, message) => {
  renderForm(); fillValidForm()
  fireEvent.change(screen.getByLabelText(new RegExp(`^${label}`)), { target: { value } })
  fireEvent.click(screen.getByRole('button', { name: 'Save check-in' }))
  expect(await screen.findByText(message)).toBeInTheDocument()
  expect(createDailyCheckIn).not.toHaveBeenCalled()
})

it('rejects a combined sleep duration greater than 1440 minutes', async () => {
  renderForm(); fillValidForm()
  fireEvent.change(screen.getByLabelText(/^Sleep hours/), { target: { value: '24' } })
  fireEvent.change(screen.getByLabelText(/^Sleep minutes/), { target: { value: '1' } })
  fireEvent.click(screen.getByRole('button', { name: 'Save check-in' }))
  expect(await screen.findByText('Combined sleep duration cannot exceed 24 hours.')).toBeInTheDocument()
  expect(createDailyCheckIn).not.toHaveBeenCalled()
})

it('resets the form values after successful submission', async () => {
  vi.mocked(createDailyCheckIn).mockResolvedValue(response)
  renderForm(); fillValidForm('Rested.')
  fireEvent.click(screen.getByRole('button', { name: 'Save check-in' }))
  await waitFor(() => expect(screen.getByLabelText(/^Sleep hours/)).toHaveValue(null))
  expect(screen.getByLabelText(/^Sleep minutes/)).toHaveValue(null)
  expect(screen.getByLabelText(/^Notes/)).toHaveValue('')
})

it('rejects notes longer than the backend limit', async () => {
  renderForm(); fillValidForm('x'.repeat(5001))
  fireEvent.click(screen.getByRole('button', { name: 'Save check-in' }))
  expect(await screen.findByText('Notes must be 5000 characters or fewer.')).toBeInTheDocument()
  expect(createDailyCheckIn).not.toHaveBeenCalled()
})

it('renders a structured duplicate-date conflict and correlation id', async () => {
  const error = new ApiClientError('Duplicate Daily Check-in', 409, 'duplicate-42', {
    timestamp: '2026-09-13T20:00:00Z', status: 409, error: 'Duplicate Daily Check-in', details: ['A daily check-in already exists for 2026-09-13'],
  })
  vi.mocked(createDailyCheckIn).mockRejectedValue(error)
  renderForm(); fillValidForm(); fireEvent.click(screen.getByRole('button', { name: 'Save check-in' }))
  expect(await screen.findByText('Duplicate Daily Check-in')).toBeInTheDocument()
  expect(screen.getByText('A daily check-in already exists for 2026-09-13')).toBeInTheDocument()
  expect(screen.getByText(/duplicate-42/)).toBeInTheDocument()
})

it('renders a generic unreachable-service failure', async () => {
  vi.mocked(createDailyCheckIn).mockRejectedValue(new Error('offline'))
  renderForm(); fillValidForm(); fireEvent.click(screen.getByRole('button', { name: 'Save check-in' }))
  expect(await screen.findByText('The service could not be reached. Check that the backend is running.')).toBeInTheDocument()
})

it('disables submission and prevents duplicate requests while pending', async () => {
  let resolveRequest!: (value: typeof response) => void
  vi.mocked(createDailyCheckIn).mockImplementation(() => new Promise((resolve) => { resolveRequest = resolve }))
  const { unmount } = renderForm(); fillValidForm()
  const submit = screen.getByRole('button', { name: 'Save check-in' })
  fireEvent.click(submit); fireEvent.click(submit)
  await waitFor(() => expect(submit).toBeDisabled())
  expect(createDailyCheckIn).toHaveBeenCalledOnce()
  resolveRequest(response)
  await waitFor(() => expect(submit).not.toBeDisabled())
  unmount()
})
