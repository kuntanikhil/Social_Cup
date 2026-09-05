export function resolveMediaUrl(path: string | null): string | null {
  if (!path) return null
  if (/^https?:\/\//i.test(path)) return path
  if (path.startsWith('/')) {
    return import.meta.env.DEV ? path : `${import.meta.env.VITE_API_URL}${path}`
  }
  return null
}
