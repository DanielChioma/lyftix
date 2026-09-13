import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRef, useState, type FormEvent, type ReactNode } from 'react'
import { createDailyCheckIn } from '../api/checkIns'
import type { CreateDailyCheckInRequest } from '../api/checkIns.types'
import { toLocalDateValue } from '../utils/checkInForm'
import { ApiErrorMessage } from './ApiErrorMessage'

interface FormValues {
  checkInDate: string
  mood: string
  energy: string
  focus: string
  stress: string
  productivity: string
  sleepHours: string
  sleepMinutes: string
  notes: string
}

type FormErrors = Partial<Record<keyof FormValues, string>>
type RatingName = 'mood' | 'energy' | 'focus' | 'stress' | 'productivity'

function initialValues(): FormValues {
  return {
    checkInDate: toLocalDateValue(new Date()),
    mood: '',
    energy: '',
    focus: '',
    stress: '',
    productivity: '',
    sleepHours: '',
    sleepMinutes: '',
    notes: '',
  }
}

function validate(values: FormValues): FormErrors {
  const errors: FormErrors = {}
  if (!values.checkInDate) errors.checkInDate = 'Choose a check-in date.'
  const ratingNames: RatingName[] = ['mood', 'energy', 'focus', 'stress', 'productivity']
  for (const name of ratingNames) {
    const value = Number(values[name])
    if (!values[name]) errors[name] = `Enter ${name} from 1 to 10.`
    else if (!Number.isInteger(value) || value < 1 || value > 10) errors[name] = `${name[0].toUpperCase()}${name.slice(1)} must be a whole number from 1 to 10.`
  }
  const sleepHours = Number(values.sleepHours)
  const sleepMinutes = Number(values.sleepMinutes)
  if (!values.sleepHours) errors.sleepHours = 'Enter sleep hours.'
  else if (!Number.isInteger(sleepHours) || sleepHours < 0) errors.sleepHours = 'Sleep hours must be a non-negative whole number.'
  if (!values.sleepMinutes) errors.sleepMinutes = 'Enter the remaining sleep minutes.'
  else if (!Number.isInteger(sleepMinutes) || sleepMinutes < 0 || sleepMinutes > 59) errors.sleepMinutes = 'Sleep minutes must be a whole number from 0 to 59.'
  if (!errors.sleepHours && !errors.sleepMinutes && (sleepHours * 60) + sleepMinutes > 1440) errors.sleepHours = 'Combined sleep duration cannot exceed 24 hours.'
  if (values.notes.length > 5000) errors.notes = 'Notes must be 5000 characters or fewer.'
  return errors
}

export function CreateDailyCheckInForm({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const queryClient = useQueryClient()
  const [values, setValues] = useState(initialValues)
  const [errors, setErrors] = useState<FormErrors>({})
  const submittingRef = useRef(false)
  const mutation = useMutation({
    mutationFn: (request: CreateDailyCheckInRequest) => createDailyCheckIn(request),
    throwOnError: false,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['check-ins'] }),
        queryClient.invalidateQueries({ queryKey: ['analytics', 'check-ins'] }),
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
    if (Object.keys(nextErrors).length) return
    const request: CreateDailyCheckInRequest = {
      checkInDate: values.checkInDate,
      mood: Number(values.mood),
      energy: Number(values.energy),
      focus: Number(values.focus),
      stress: Number(values.stress),
      productivity: Number(values.productivity),
      sleepMinutes: (Number(values.sleepHours) * 60) + Number(values.sleepMinutes),
      notes: values.notes.trim() === '' ? null : values.notes,
    }
    submittingRef.current = true
    void mutation.mutateAsync(request).catch(() => undefined).finally(() => {
      submittingRef.current = false
    })
  }

  return <section className="create-check-in-panel" aria-labelledby="create-check-in-title">
    <div className="section-heading"><div><h2 id="create-check-in-title">Add daily check-in</h2><p>Record today’s wellbeing, focus, sleep, and productivity.</p></div><button className="secondary-button" type="button" onClick={onClose} disabled={mutation.isPending}>Close</button></div>
    <form className="create-check-in-form" onSubmit={submit} noValidate>
      <Field label="Check-in date" error={errors.checkInDate}><input type="date" value={values.checkInDate} onChange={(event) => update('checkInDate', event.target.value)} aria-invalid={Boolean(errors.checkInDate)} required /></Field>
      <RatingField name="mood" label="Mood" value={values.mood} error={errors.mood} update={update} />
      <RatingField name="energy" label="Energy" value={values.energy} error={errors.energy} update={update} />
      <RatingField name="focus" label="Focus" value={values.focus} error={errors.focus} update={update} />
      <RatingField name="stress" label="Stress" value={values.stress} error={errors.stress} update={update} />
      <RatingField name="productivity" label="Productivity" value={values.productivity} error={errors.productivity} update={update} />
      <Field label="Sleep hours (whole hours)" hint="0 to 24 hours" error={errors.sleepHours}><input type="number" min="0" max="24" step="1" inputMode="numeric" value={values.sleepHours} onChange={(event) => update('sleepHours', event.target.value)} aria-invalid={Boolean(errors.sleepHours)} required /></Field>
      <Field label="Sleep minutes (remaining minutes)" hint="0 to 59 minutes" error={errors.sleepMinutes}><input type="number" min="0" max="59" step="1" inputMode="numeric" value={values.sleepMinutes} onChange={(event) => update('sleepMinutes', event.target.value)} aria-invalid={Boolean(errors.sleepMinutes)} required /></Field>
      <Field label="Notes (optional)" hint="Up to 5000 characters" error={errors.notes} wide><textarea maxLength={5000} rows={4} value={values.notes} onChange={(event) => update('notes', event.target.value)} aria-invalid={Boolean(errors.notes)} /></Field>
      {mutation.error && <div className="create-check-in-error"><ApiErrorMessage error={mutation.error} /></div>}
      <div className="form-actions"><button className="primary-button" type="submit" disabled={mutation.isPending}>{mutation.isPending ? 'Saving check-in…' : 'Save check-in'}</button><button className="secondary-button" type="button" onClick={onClose} disabled={mutation.isPending}>Cancel</button></div>
    </form>
  </section>
}

function RatingField({ name, label, value, error, update }: { name: RatingName; label: string; value: string; error?: string; update: (name: keyof FormValues, value: string) => void }) {
  return <Field label={`${label} (1–10)`} hint="1 (low) to 10 (high)" error={error}><input type="number" min="1" max="10" step="1" inputMode="numeric" value={value} onChange={(event) => update(name, event.target.value)} aria-invalid={Boolean(error)} required /></Field>
}

function Field({ label, hint, error, wide, children }: { label: string; hint?: string; error?: string; wide?: boolean; children: ReactNode }) {
  return <label className={`check-in-field${wide ? ' check-in-field-wide' : ''}`}><span>{label}</span>{children}{hint && <small>{hint}</small>}{error && <small className="field-error" role="alert">{error}</small>}</label>
}
