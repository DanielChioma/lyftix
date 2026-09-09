import type { ReactNode } from 'react'
import { ApiErrorMessage } from './ApiErrorMessage'

export function AnalyticsSection({ title, isPending, error, children }: { title: string; isPending: boolean; error: unknown; children: ReactNode }) {
  const id = `${title.toLowerCase().replaceAll(' ', '-')}-title`
  return <section className="analytics-section" aria-labelledby={id}>
    <h2 id={id}>{title}</h2>
    {isPending ? <div className="kpi-grid" aria-label={`Loading ${title}`}><span className="skeleton" /><span className="skeleton" /><span className="skeleton" /></div> : error ? <ApiErrorMessage error={error} /> : children}
  </section>
}
