import type { PageResponse, WorkoutMetricResponse, WorkoutSort } from '../api/workouts.types'
import { durationBetween, formatDateTimeDate, formatDuration, formatNumber, formatTime } from '../utils/format'

function WorkoutValues({ workout }: { workout: WorkoutMetricResponse }) {
  return <>
    <span data-label="Date">{formatDateTimeDate(workout.startedAt)}</span>
    <strong data-label="Type">{workout.workoutType}</strong>
    <span data-label="Start">{formatTime(workout.startedAt)}</span>
    <span data-label="Duration">{formatDuration(durationBetween(workout.startedAt, workout.endedAt))}</span>
    <span data-label="Calories">{formatNumber(workout.caloriesBurned)}</span>
    <span data-label="Intensity">{workout.intensity}/10</span>
  </>
}

export function WorkoutHistory({ page, sortBy, onPageChange, onSizeChange, onSortChange }: {
  page: PageResponse<WorkoutMetricResponse>; sortBy: WorkoutSort
  onPageChange: (page: number) => void; onSizeChange: (size: number) => void; onSortChange: (sort: WorkoutSort) => void
}) {
  return <>
    <div className="history-controls">
      <label><span>Sort workouts</span><select value={sortBy} onChange={(event) => onSortChange(event.target.value as WorkoutSort)}>
        <option value="startedAt">Newest first</option><option value="caloriesBurned">Highest calories</option><option value="intensity">Highest intensity</option>
      </select></label>
      <label><span>Page size</span><select value={page.size} onChange={(event) => onSizeChange(Number(event.target.value))}>
        <option value="10">10</option><option value="20">20</option>
      </select></label>
    </div>

    {page.empty ? <p className="empty-state">No workouts recorded in this date range.</p> : <>
      <div className="workout-table-wrap"><table className="workout-table">
        <thead><tr><th scope="col">Date</th><th scope="col">Type</th><th scope="col">Start</th><th scope="col">Duration</th><th scope="col">Calories</th><th scope="col">Intensity</th></tr></thead>
        <tbody>{page.content.map((workout) => <tr key={workout.id}><td>{formatDateTimeDate(workout.startedAt)}</td><td>{workout.workoutType}</td><td>{formatTime(workout.startedAt)}</td><td>{formatDuration(durationBetween(workout.startedAt, workout.endedAt))}</td><td>{formatNumber(workout.caloriesBurned)}</td><td>{workout.intensity}/10</td></tr>)}</tbody>
      </table></div>
      <div className="workout-cards">{page.content.map((workout) => <article key={workout.id} aria-label={`${workout.workoutType} workout`}><WorkoutValues workout={workout} /></article>)}</div>
    </>}

    <nav className="pagination" aria-label="Workout history pagination">
      <button type="button" disabled={page.first} onClick={() => onPageChange(page.number - 1)} aria-label="Previous workout page">Previous</button>
      <span>Page {page.totalPages === 0 ? 0 : page.number + 1} of {page.totalPages}</span>
      <button type="button" disabled={page.last || page.totalPages === 0} onClick={() => onPageChange(page.number + 1)} aria-label="Next workout page">Next</button>
    </nav>
  </>
}
