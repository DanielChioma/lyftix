import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <section className="not-found">
      <p className="eyebrow">404</p>
      <h1>Page not found</h1>
      <p className="lede">The page you requested does not exist.</p>
      <Link className="button-link" to="/">Return to dashboard</Link>
    </section>
  )
}
