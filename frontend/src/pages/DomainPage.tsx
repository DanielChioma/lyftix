interface DomainPageProps {
  title: string
  description: string
}

export function DomainPage({ title, description }: DomainPageProps) {
  return (
    <section className="placeholder-page">
      <p className="eyebrow">Lyftix data</p>
      <h1>{title}</h1>
      <p className="lede">{description}</p>
      <div className="empty-state">
        <h2>Dashboard coming next</h2>
        <p>This foundation is ready for real API-backed data. No sample data is shown.</p>
      </div>
    </section>
  )
}
