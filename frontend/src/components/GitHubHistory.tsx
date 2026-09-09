import type { GitHubActivityResponse, GitHubSort, PageResponse } from '../api/github.types'
import { formatDateTimeDate, formatTime, formatNumber } from '../utils/format'
import { formatGitHubActivityType } from '../utils/github'

function Repository({ activity }: { activity: GitHubActivityResponse }) { return <>{activity.repositoryOwner}/{activity.repositoryName}</> }

function ActivityValues({ activity }: { activity: GitHubActivityResponse }) {
  return <><span data-label="Date">{formatDateTimeDate(activity.occurredAt)} at {formatTime(activity.occurredAt)}</span><strong data-label="Activity">{formatGitHubActivityType(activity.activityType)}</strong><span data-label="Repository" className="wrap-value"><Repository activity={activity} /></span><span data-label="Title" className="wrap-value">{activity.title}</span></>
}

export function GitHubHistory({ page, sortBy, onPageChange, onSizeChange, onSortChange }: {
  page: PageResponse<GitHubActivityResponse>; sortBy: GitHubSort
  onPageChange: (page: number) => void; onSizeChange: (size: number) => void; onSortChange: (sort: GitHubSort) => void
}) {
  return <>
    <div className="history-controls"><label><span>Sort activities</span><select value={sortBy} onChange={(event) => onSortChange(event.target.value as GitHubSort)}><option value="occurredAt">Newest first</option><option value="activityType">Activity type Z–A</option><option value="repositoryName">Repository Z–A</option></select></label><label><span>Page size</span><select value={page.size} onChange={(event) => onSizeChange(Number(event.target.value))}><option value="10">10</option><option value="20">20</option></select></label></div>
    {page.empty ? <p className="empty-state">No GitHub activity was recorded in this date range.</p> : <><div className="github-table-wrap"><table className="github-table"><thead><tr><th scope="col">Occurred</th><th scope="col">Activity</th><th scope="col">Repository</th><th scope="col">Title</th></tr></thead><tbody>{page.content.map((activity) => <tr key={activity.id}><td>{formatDateTimeDate(activity.occurredAt)} at {formatTime(activity.occurredAt)}</td><td>{formatGitHubActivityType(activity.activityType)}</td><td className="wrap-value"><Repository activity={activity} /></td><td className="wrap-value">{activity.title}</td></tr>)}</tbody></table></div><div className="github-cards">{page.content.map((activity) => <article key={activity.id} aria-label={`${formatGitHubActivityType(activity.activityType)} GitHub activity`}><ActivityValues activity={activity} /></article>)}</div></>}
    <nav className="pagination" aria-label="GitHub activity history pagination"><button type="button" disabled={page.first} onClick={() => onPageChange(page.number - 1)} aria-label="Previous GitHub activity page">Previous</button><span>Page {page.totalPages === 0 ? 0 : page.number + 1} of {page.totalPages} · {formatNumber(page.totalElements)} total</span><button type="button" disabled={page.last || page.totalPages === 0} onClick={() => onPageChange(page.number + 1)} aria-label="Next GitHub activity page">Next</button></nav>
  </>
}
