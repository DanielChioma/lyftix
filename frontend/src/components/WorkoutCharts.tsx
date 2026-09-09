import type { ReactNode } from 'react'
import { Bar, BarChart, CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { WorkoutAnalyticsResponse } from '../api/analytics.types'
import { formatChartDate } from '../utils/format'
import { workoutTrendData, workoutTypeData } from '../utils/workoutChartData'

function ChartCard({ title, description, empty, children }: { title: string; description: string; empty: boolean; children: ReactNode }) {
  const id = `${title.toLowerCase().replaceAll(' ', '-')}-title`
  return <section className="chart-card" aria-labelledby={id}>
    <div className="chart-heading"><h2 id={id}>{title}</h2><p>{description}</p></div>
    {empty ? <p className="empty-state">No workout data recorded in this range.</p> : children}
  </section>
}

export function WorkoutCharts({ analytics }: { analytics: WorkoutAnalyticsResponse }) {
  const trend = workoutTrendData(analytics)
  const types = workoutTypeData(analytics)

  return <div className="workout-charts">
    <ChartCard title="Workout frequency" description="Number of workouts started each day." empty={trend.length === 0}>
      <div className="chart-container"><ResponsiveContainer width="100%" height="100%"><BarChart data={trend} accessibilityLayer>
        <CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="date" tickFormatter={formatChartDate} /><YAxis allowDecimals={false} />
        <Tooltip labelFormatter={(value) => formatChartDate(String(value))} /><Legend /><Bar dataKey="workoutCount" name="Workouts" fill="#257347" />
      </BarChart></ResponsiveContainer></div>
    </ChartCard>
    <ChartCard title="Workout duration" description="Total workout minutes by day." empty={trend.length === 0}>
      <div className="chart-container"><ResponsiveContainer width="100%" height="100%"><LineChart data={trend} accessibilityLayer>
        <CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="date" tickFormatter={formatChartDate} /><YAxis unit="m" />
        <Tooltip labelFormatter={(value) => formatChartDate(String(value))} formatter={(value) => [`${value}m`]} /><Legend />
        <Line dataKey="durationMinutes" name="Workout minutes" stroke="#3f6da6" strokeWidth={2} />
      </LineChart></ResponsiveContainer></div>
    </ChartCard>
    <ChartCard title="Calories burned" description="Estimated calories burned by day." empty={trend.length === 0}>
      <div className="chart-container"><ResponsiveContainer width="100%" height="100%"><LineChart data={trend} accessibilityLayer>
        <CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="date" tickFormatter={formatChartDate} /><YAxis />
        <Tooltip labelFormatter={(value) => formatChartDate(String(value))} /><Legend />
        <Line dataKey="caloriesBurned" name="Calories" stroke="#b06b2c" strokeDasharray="6 3" strokeWidth={2} />
      </LineChart></ResponsiveContainer></div>
    </ChartCard>
    <ChartCard title="Workout types" description="Workout count grouped by recorded type." empty={types.length === 0}>
      <div className="chart-container"><ResponsiveContainer width="100%" height="100%"><BarChart data={types} accessibilityLayer layout="vertical">
        <CartesianGrid strokeDasharray="3 3" horizontal={false} /><XAxis type="number" allowDecimals={false} /><YAxis type="category" dataKey="workoutType" width={90} />
        <Tooltip /><Legend /><Bar dataKey="count" name="Workouts by type" fill="#6a4c93" />
      </BarChart></ResponsiveContainer></div>
    </ChartCard>
  </div>
}
