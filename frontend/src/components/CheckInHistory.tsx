import type { CheckInPage, CheckInSort, DailyCheckInResponse } from '../api/checkIns.types'
import { formatSleepMinutes } from '../utils/checkIns'

function Rating({ label, value }: { label: string; value: number }) {
  return <span data-label={label}>{value} / 10</span>
}

function CardValues({ checkIn }: { checkIn: DailyCheckInResponse }) {
  return <>
    <strong data-label="Date">{checkIn.checkInDate}</strong>
    <Rating label="Mood" value={checkIn.mood} />
    <Rating label="Energy" value={checkIn.energy} />
    <Rating label="Focus" value={checkIn.focus} />
    <Rating label="Stress" value={checkIn.stress} />
    <Rating label="Productivity" value={checkIn.productivity} />
    <span data-label="Sleep">{formatSleepMinutes(checkIn.sleepMinutes)}</span>
    <span data-label="Notes" className="check-in-notes">{checkIn.notes ?? '—'}</span>
  </>
}

export function CheckInHistory({ page, sortBy, onPageChange, onSizeChange, onSortChange }: {
  page: CheckInPage
  sortBy: CheckInSort
  onPageChange: (page: number) => void
  onSizeChange: (size: number) => void
  onSortChange: (sort: CheckInSort) => void
}) {
  return <>
    <div className="history-controls">
      <label><span>Sort check-ins</span><select value={sortBy} onChange={(event) => onSortChange(event.target.value as CheckInSort)}>
        <option value="checkInDate">Newest date</option>
        <option value="mood">Highest mood</option>
        <option value="energy">Highest energy</option>
        <option value="focus">Highest focus</option>
        <option value="stress">Highest stress</option>
        <option value="productivity">Highest productivity</option>
        <option value="sleepMinutes">Most sleep</option>
      </select></label>
      <label><span>Page size</span><select value={page.size} onChange={(event) => onSizeChange(Number(event.target.value))}>
        <option value="10">10</option><option value="20">20</option>
      </select></label>
    </div>
    {page.empty ? <p className="empty-state">No daily check-ins were recorded in this range.</p> : <>
      <div className="check-in-table-wrap"><table className="check-in-table">
        <thead><tr><th scope="col">Date</th><th scope="col">Mood</th><th scope="col">Energy</th><th scope="col">Focus</th><th scope="col">Stress</th><th scope="col">Productivity</th><th scope="col">Sleep</th><th scope="col">Notes</th></tr></thead>
        <tbody>{page.content.map((checkIn) => <tr key={checkIn.id}>
          <td>{checkIn.checkInDate}</td><td>{checkIn.mood} / 10</td><td>{checkIn.energy} / 10</td><td>{checkIn.focus} / 10</td><td>{checkIn.stress} / 10</td><td>{checkIn.productivity} / 10</td><td>{formatSleepMinutes(checkIn.sleepMinutes)}</td><td className="check-in-notes">{checkIn.notes ?? '—'}</td>
        </tr>)}</tbody>
      </table></div>
      <div className="check-in-cards">{page.content.map((checkIn) => <article key={checkIn.id} aria-label={`${checkIn.checkInDate} daily check-in`}><CardValues checkIn={checkIn} /></article>)}</div>
    </>}
    <nav className="pagination" aria-label="Daily check-in history pagination">
      <button type="button" disabled={page.first} onClick={() => onPageChange(page.number - 1)} aria-label="Previous check-in page">Previous</button>
      <span>Page {page.totalPages ? page.number + 1 : 0} of {page.totalPages} · {page.totalElements} total</span>
      <button type="button" disabled={page.last || !page.totalPages} onClick={() => onPageChange(page.number + 1)} aria-label="Next check-in page">Next</button>
    </nav>
  </>
}
