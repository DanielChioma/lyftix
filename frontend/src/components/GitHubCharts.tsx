import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { GitHubAnalyticsResponse } from '../api/analytics.types'
import { formatChartDate } from '../utils/format'
import { formatGitHubActivityType, percentage, rankCounts, sortDailyCounts } from '../utils/github'

export function GitHubCharts({ analytics }: { analytics: GitHubAnalyticsResponse }) {
  const daily = sortDailyCounts(analytics.daily)
  const activityTypes = rankCounts(analytics.countsByActivityType, (item) => item.activityType)
    .map((item) => ({ ...item, label: formatGitHubActivityType(item.activityType) }))
  const repositories = rankCounts(analytics.countsByRepository, (item) => item.repository)

  return <div className="github-charts">
    <section className="chart-card"><div className="chart-heading"><h2>Daily GitHub activity</h2><p>Recorded GitHub activity count per day.</p></div>{daily.length === 0 ? <p className="section-empty">No GitHub activity was recorded in this range.</p> : <div className="chart-container"><ResponsiveContainer width="100%" height="100%"><LineChart data={daily}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="date" tickFormatter={formatChartDate} /><YAxis allowDecimals={false} /><Tooltip labelFormatter={(value) => formatChartDate(String(value))} /><Line dataKey="activityCount" name="Activities" stroke="var(--chart-green)" strokeWidth={2} /></LineChart></ResponsiveContainer></div>}</section>
    <section className="chart-card"><div className="chart-heading"><h2>Activity by type</h2><p>Recorded events ranked by activity count.</p></div>{activityTypes.length === 0 ? <p className="section-empty">No activity-type data is available for this range.</p> : <><div className="chart-container"><ResponsiveContainer width="100%" height="100%"><BarChart data={activityTypes.slice(0, 8)} layout="vertical" margin={{ left: 24 }}><CartesianGrid strokeDasharray="3 3" /><XAxis type="number" allowDecimals={false} /><YAxis type="category" dataKey="label" width={120} /><Tooltip /><Bar dataKey="count" name="Activities" fill="var(--chart-blue)" /></BarChart></ResponsiveContainer></div><ol className="ranked-list">{activityTypes.map((item) => <li key={item.activityType}><span>{item.label}</span><strong>{item.count}</strong></li>)}</ol></>}</section>
    <section className="chart-card github-repositories"><div className="chart-heading"><h2>Repository activity</h2><p>Top repositories by activity; the complete ranked breakdown appears below.</p></div>{repositories.length === 0 ? <p className="section-empty">No repository activity is available for this range.</p> : <><div className="chart-container"><ResponsiveContainer width="100%" height="100%"><BarChart data={repositories.slice(0, 8)} layout="vertical" margin={{ left: 24 }}><CartesianGrid strokeDasharray="3 3" /><XAxis type="number" allowDecimals={false} /><YAxis type="category" dataKey="repository" width={130} /><Tooltip /><Bar dataKey="count" name="Activities" fill="var(--chart-orange)" /></BarChart></ResponsiveContainer></div><ol className="ranked-list">{repositories.map((item) => <li key={item.repository}><span>{item.repository}</span><strong>{item.count} · {percentage(item.count, analytics.totalActivities)?.toFixed(1)}%</strong></li>)}</ol></>}</section>
  </div>
}
