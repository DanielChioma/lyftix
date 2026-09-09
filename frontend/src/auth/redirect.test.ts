import { getSafeDestination } from './redirect'

it('preserves approved internal destinations and their query strings', () => {
  expect(getSafeDestination('/workouts?range=30d')).toBe('/workouts?range=30d')
})

it.each(['https://example.com', '//example.com', '/admin', undefined])('rejects unsafe destination %s', (destination) => {
  expect(getSafeDestination(destination)).toBe('/')
})
