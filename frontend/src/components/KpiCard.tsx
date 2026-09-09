export function KpiCard({ label, value }: { label: string; value: string | number }) {
  return <article className="kpi-card" aria-label={`${label}: ${value}`}><p>{label}</p><strong>{value}</strong></article>
}
