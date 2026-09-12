import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ApiClientError } from '../api/client'
import { createWorkout } from '../api/workouts'
import { CreateWorkoutForm } from './CreateWorkoutForm'

vi.mock('../api/workouts', () => ({ createWorkout: vi.fn() }))

function renderForm() {
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false }, queries: { retry: false } } })
  const invalidate = vi.spyOn(client, 'invalidateQueries').mockResolvedValue()
  const onClose = vi.fn()
  const onCreated = vi.fn()
  const view = render(<QueryClientProvider client={client}><CreateWorkoutForm onClose={onClose} onCreated={onCreated} /></QueryClientProvider>)
  return { invalidate, onClose, onCreated, unmount: view.unmount }
}

function fillValidForm() {
  fireEvent.change(screen.getByLabelText('Workout type'), { target: { value: 'Cycling' } })
  fireEvent.change(screen.getByLabelText('Started at'), { target: { value: '2026-01-15T10:30' } })
  fireEvent.change(screen.getByLabelText('Ended at'), { target: { value: '2026-01-15T11:45' } })
  fireEvent.change(screen.getByLabelText(/^Intensity/), { target: { value: '7' } })
  fireEvent.change(screen.getByLabelText(/^Calories burned/), { target: { value: '350' } })
}

beforeEach(() => { vi.mocked(createWorkout).mockReset() })

it('submits converted values, invalidates dependent data, and closes after success', async () => {
  vi.mocked(createWorkout).mockResolvedValue({
    id: 1, workoutType: 'Cycling', intensity: 7, caloriesBurned: 350,
    startedAt: '2026-01-15T10:30:00.000Z', endedAt: '2026-01-15T11:45:00.000Z',
    createdAt: '2026-01-15T11:46:00Z', updatedAt: '2026-01-15T11:46:00Z',
  })
  const { invalidate, onClose, onCreated } = renderForm()
  fillValidForm()
  fireEvent.click(screen.getByRole('button', { name: 'Save workout' }))

  await waitFor(() => expect(vi.mocked(createWorkout).mock.calls[0]?.[0]).toEqual({
    workoutType: 'Cycling', intensity: 7, caloriesBurned: 350,
    startedAt: '2026-01-15T10:30:00.000Z', endedAt: '2026-01-15T11:45:00.000Z',
  }))
  await waitFor(() => expect(onClose).toHaveBeenCalledOnce())
  expect(onCreated).toHaveBeenCalledOnce()
  expect(invalidate).toHaveBeenCalledWith({ queryKey: ['workouts'] })
  expect(invalidate).toHaveBeenCalledWith({ queryKey: ['analytics', 'workouts'] })
  expect(invalidate).toHaveBeenCalledWith({ queryKey: ['analytics', 'daily-summary'] })
})

it('validates required fields and the backend-required calories field without submitting', async () => {
  renderForm()
  fireEvent.change(screen.getByLabelText('Started at'), { target: { value: '' } })
  fireEvent.change(screen.getByLabelText('Ended at'), { target: { value: '' } })
  fireEvent.click(screen.getByRole('button', { name: 'Save workout' }))

  expect(await screen.findByText('Choose a workout type.')).toBeInTheDocument()
  expect(screen.getByText('Enter a start date and time.')).toBeInTheDocument()
  expect(screen.getByText('Enter an end date and time.')).toBeInTheDocument()
  expect(screen.getByText('Enter an intensity from 1 to 10.')).toBeInTheDocument()
  expect(screen.getByText('Enter estimated calories burned.')).toBeInTheDocument()
  expect(createWorkout).not.toHaveBeenCalled()
})

it.each([
  ['2026-01-15T10:30', 'equal'],
  ['2026-01-15T10:00', 'before'],
])('rejects an end time that is %s the start time', async (endedAt) => {
  renderForm()
  fillValidForm()
  fireEvent.change(screen.getByLabelText('Ended at'), { target: { value: endedAt } })
  fireEvent.click(screen.getByRole('button', { name: 'Save workout' }))
  expect(await screen.findByText('End time must be after start time.')).toBeInTheDocument()
  expect(createWorkout).not.toHaveBeenCalled()
})

it.each(['0', '11', '2.5'])('rejects invalid intensity %s', async (intensity) => {
  renderForm()
  fillValidForm()
  fireEvent.change(screen.getByLabelText(/^Intensity/), { target: { value: intensity } })
  fireEvent.click(screen.getByRole('button', { name: 'Save workout' }))
  expect(await screen.findByText('Intensity must be a whole number from 1 to 10.')).toBeInTheDocument()
  expect(createWorkout).not.toHaveBeenCalled()
})

it.each(['-1', '2.5'])('rejects invalid calories %s', async (calories) => {
  renderForm()
  fillValidForm()
  fireEvent.change(screen.getByLabelText(/^Calories burned/), { target: { value: calories } })
  fireEvent.click(screen.getByRole('button', { name: 'Save workout' }))
  expect(await screen.findByText('Calories must be a whole number of 0 or more.')).toBeInTheDocument()
  expect(createWorkout).not.toHaveBeenCalled()
})

it('shows structured API failures including the correlation id', async () => {
  vi.mocked(createWorkout).mockRejectedValue(new ApiClientError('Validation Failed', 400, 'request-42', {
    timestamp: '2026-01-15T10:00:00Z', status: 400, error: 'Validation Failed', details: ['Workout data was rejected'],
  }))
  renderForm()
  fillValidForm()
  fireEvent.click(screen.getByRole('button', { name: 'Save workout' }))
  expect(await screen.findByText('Validation Failed')).toBeInTheDocument()
  expect(screen.getByText('Workout data was rejected')).toBeInTheDocument()
  expect(screen.getByText(/request-42/)).toBeInTheDocument()
})

it('disables submission and prevents duplicate requests while pending', async () => {
  vi.mocked(createWorkout).mockImplementation(() => new Promise(() => undefined))
  const { unmount } = renderForm()
  fillValidForm()
  const submit = screen.getByRole('button', { name: 'Save workout' })
  fireEvent.click(submit)
  fireEvent.click(submit)
  await waitFor(() => expect(submit).toBeDisabled())
  expect(createWorkout).toHaveBeenCalledOnce()
  unmount()
})
