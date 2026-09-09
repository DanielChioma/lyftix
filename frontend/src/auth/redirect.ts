const PROTECTED_PATHS = new Set(['/', '/workouts', '/productivity', '/github', '/coding', '/check-ins', '/system'])

export function getSafeDestination(value: unknown) {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')) {
    return '/'
  }

  const pathname = value.split(/[?#]/, 1)[0]
  return PROTECTED_PATHS.has(pathname) ? value : '/'
}
