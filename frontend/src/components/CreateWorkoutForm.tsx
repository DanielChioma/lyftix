import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRef, useState, type FormEvent, type ReactNode } from 'react'
import { createWorkout } from '../api/workouts'
import type { CreateWorkoutMetricRequest } from '../api/workouts.types'
import { localDateTimeToInstant, toLocalDateTimeValue } from '../utils/workoutForm'
import { ApiErrorMessage } from './ApiErrorMessage'

const WORKOUT_TYPES = ['Running', 'Strength', 'Cycling', 'Walking', 'Swimming', 'Other']

interface FormValues {
  workoutType: string
  startedAt: string
  endedAt: string
  intensity: string
  caloriesBurned: string
}

type FormErrors = Partial<Record<keyof FormValues, string>>

function initialValues(): FormValues {
  const now = new Date()
  return {
    workoutType: '',
    startedAt: toLocalDateTimeValue(now),
    endedAt: toLocalDateTimeValue(new Date(now.getTime() + 60 * 60_000)),
    intensity: '',
    caloriesBurned: '',
  }
}

function validate(values: FormValues): FormErrors {
  const errors: FormErrors = {}
  if (!values.workoutType) errors.workoutType = 'Choose a workout type.'
  if (!values.startedAt) errors.startedAt = 'Enter a start date and time.'
  if (!values.endedAt) errors.endedAt = 'Enter an end date and time.'
  if (values.startedAt && values.endedAt && new Date(values.endedAt) <= new Date(values.startedAt)) {
    errors.endedAt = 'End time must be after start time.'
  }
  const intensity = Number(values.intensity)
  if (!values.intensity) errors.intensity = 'Enter an intensity from 1 to 10.'
  else if (!Number.isInteger(intensity) || intensity < 1 || intensity > 10) errors.intensity = 'Intensity must be a whole number from 1 to 10.'
  const calories = Number(values.caloriesBurned)
  if (!values.caloriesBurned) errors.caloriesBurned = 'Enter estimated calories burned.'
  else if (!Number.isInteger(calories) || calories < 0) errors.caloriesBurned = 'Calories must be a whole number of 0 or more.'
  return errors
}

export function CreateWorkoutForm({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const queryClient = useQueryClient()
  const [values, setValues] = useState(initialValues)
  const [errors, setErrors] = useState<FormErrors>({})
  const submittingRef = useRef(false)
  const mutation = useMutation({
    mutationFn: (request: CreateWorkoutMetricRequest) => createWorkout(request),
    throwOnError: false,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['workouts'] }),
        queryClient.invalidateQueries({ queryKey: ['analytics', 'workouts'] }),
        queryClient.invalidateQueries({ queryKey: ['analytics', 'daily-summary'] }),
      ])
      setValues(initialValues())
      onCreated()
      onClose()
    },
    onError: () => undefined,
  })

  function update(name: keyof FormValues, value: string) {
    setValues((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: undefined }))
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submittingRef.current) return
    const nextErrors = validate(values)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    const request: CreateWorkoutMetricRequest = {
      workoutType: values.workoutType,
      startedAt: localDateTimeToInstant(values.startedAt),
      endedAt: localDateTimeToInstant(values.endedAt),
      intensity: Number(values.intensity),
      caloriesBurned: Number(values.caloriesBurned),
    }
    submittingRef.current = true
    void mutation.mutateAsync(request).catch(() => undefined).finally(() => {
      submittingRef.current = false
    })
  }

  return (
    <section className="create-workout-panel" aria-labelledby="create-workout-title">
      <div className="section-heading">
        <div><h2 id="create-workout-title">Add workout</h2><p>Record the activity, timing, effort, and estimated calories.</p></div>
        <button className="secondary-button" type="button" onClick={onClose} disabled={mutation.isPending}>Close</button>
      </div>
      <form className="create-workout-form" onSubmit={submit} noValidate>
        <FormField label="Workout type" error={errors.workoutType}>
          <select value={values.workoutType} onChange={(event) => update('workoutType', event.target.value)} aria-invalid={Boolean(errors.workoutType)} required>
            <option value="">Select a workout</option>
            {WORKOUT_TYPES.map((type) => <option key={type}>{type}</option>)}
          </select>
        </FormField>
        <FormField label="Started at" error={errors.startedAt}>
          <input type="datetime-local" value={values.startedAt} onChange={(event) => update('startedAt', event.target.value)} aria-invalid={Boolean(errors.startedAt)} required />
        </FormField>
        <FormField label="Ended at" error={errors.endedAt}>
          <input type="datetime-local" value={values.endedAt} onChange={(event) => update('endedAt', event.target.value)} aria-invalid={Boolean(errors.endedAt)} required />
        </FormField>
        <FormField label="Intensity" hint="1 (very light) to 10 (maximum effort)" error={errors.intensity}>
          <input type="number" min="1" max="10" step="1" inputMode="numeric" value={values.intensity} onChange={(event) => update('intensity', event.target.value)} aria-invalid={Boolean(errors.intensity)} required />
        </FormField>
        <FormField label="Calories burned" hint="Required by the current API." error={errors.caloriesBurned}>
          <input type="number" min="0" step="1" inputMode="numeric" value={values.caloriesBurned} onChange={(event) => update('caloriesBurned', event.target.value)} aria-invalid={Boolean(errors.caloriesBurned)} required />
        </FormField>
        {mutation.error && <div className="create-workout-error"><ApiErrorMessage error={mutation.error} /></div>}
        <div className="form-actions">
          <button className="primary-button" type="submit" disabled={mutation.isPending}>{mutation.isPending ? 'Saving workout…' : 'Save workout'}</button>
          <button className="secondary-button" type="button" onClick={onClose} disabled={mutation.isPending}>Cancel</button>
        </div>
      </form>
    </section>
  )
}

function FormField({ label, hint, error, children }: { label: string; hint?: string; error?: string; children: ReactNode }) {
  return <label className="workout-field"><span>{label}</span>{children}{hint && <small>{hint}</small>}{error && <small className="field-error" role="alert">{error}</small>}</label>
}
