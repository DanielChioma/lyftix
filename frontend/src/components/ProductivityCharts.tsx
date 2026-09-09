import { Bar, ComposedChart, CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { DailyAnalyticsSummary } from '../api/analytics.types'
import { formatChartDate } from '../utils/format'
import { secondsToHours, sortDailySummary } from '../utils/productivityAnalysis'

export function ProductivityCharts({ daily }: { daily: DailyAnalyticsSummary[] }) {
  const data = sortDailySummary(daily).map((day) => ({
    ...day,
    codingHours: secondsToHours(day.codingDurationSeconds),
    workoutHours: secondsToHours(day.workoutDurationSeconds),
  }))
  const recordedDays = data.filter((day) => day.productivity !== null)

  if (data.length === 0) return <section className="chart-card"><div className="chart-heading"><h2>Daily patterns</h2></div><p className="section-empty">No daily analytics recorded in this range.</p></section>

  return <div className="productivity-charts">
    <section className="chart-card"><div className="chart-heading"><h2>Daily check-in trend</h2><p>Recorded productivity, focus, and energy scores (1–10).</p></div>
      {recordedDays.length === 0 ? <p className="section-empty">No check-ins recorded in this range.</p> : <div className="chart-container"><ResponsiveContainer width="100%" height="100%"><LineChart data={data} margin={{ left: 0, right: 8 }}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="date" tickFormatter={formatChartDate} /><YAxis domain={[1, 10]} label={{ value: 'Score (1–10)', angle: -90, position: 'insideLeft' }} /><Tooltip labelFormatter={(value) => formatChartDate(String(value))} /><Legend /><Line connectNulls={false} dataKey="productivity" name="Productivity" stroke="var(--chart-green)" strokeWidth={2} /><Line connectNulls={false} dataKey="focus" name="Focus" stroke="var(--chart-blue)" strokeDasharray="5 3" /><Line connectNulls={false} dataKey="energy" name="Energy" stroke="var(--chart-orange)" strokeDasharray="2 3" /></LineChart></ResponsiveContainer></div>}
    </section>
    <section className="chart-card"><div className="chart-heading"><h2>Daily activity</h2><p>Duration in hours (left axis); GitHub activity count (right axis).</p></div><div className="chart-container"><ResponsiveContainer width="100%" height="100%"><ComposedChart data={data} margin={{ left: 0, right: 8 }}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="date" tickFormatter={formatChartDate} /><YAxis yAxisId="hours" label={{ value: 'Hours', angle: -90, position: 'insideLeft' }} /><YAxis yAxisId="count" orientation="right" allowDecimals={false} label={{ value: 'GitHub count', angle: 90, position: 'insideRight' }} /><Tooltip labelFormatter={(value) => formatChartDate(String(value))} /><Legend /><Bar yAxisId="hours" dataKey="codingHours" name="Coding hours" fill="var(--chart-blue)" /><Bar yAxisId="hours" dataKey="workoutHours" name="Workout hours" fill="var(--chart-orange)" /><Line yAxisId="count" dataKey="githubActivityCount" name="GitHub activities" stroke="var(--chart-green)" strokeWidth={2} /></ComposedChart></ResponsiveContainer></div></section>
    <section className="chart-card"><div className="chart-heading"><h2>Productivity and coding time</h2><p>Recorded productivity (left axis) alongside coding hours (right axis). Descriptive only.</p></div>
      {recordedDays.length === 0 ? <p className="section-empty">No productivity scores recorded in this range.</p> : <div className="chart-container"><ResponsiveContainer width="100%" height="100%"><ComposedChart data={data} margin={{ left: 0, right: 8 }}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="date" tickFormatter={formatChartDate} /><YAxis yAxisId="score" domain={[1, 10]} label={{ value: 'Productivity', angle: -90, position: 'insideLeft' }} /><YAxis yAxisId="hours" orientation="right" label={{ value: 'Coding hours', angle: 90, position: 'insideRight' }} /><Tooltip labelFormatter={(value) => formatChartDate(String(value))} /><Legend /><Line yAxisId="score" connectNulls={false} dataKey="productivity" name="Productivity" stroke="var(--chart-green)" strokeWidth={2} /><Bar yAxisId="hours" dataKey="codingHours" name="Coding hours" fill="var(--chart-blue)" /></ComposedChart></ResponsiveContainer></div>}
    </section>
  </div>
}
