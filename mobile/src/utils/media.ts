import { API_BASE_URL } from '@/src/api/apiClient';

export function resolveMediaUrl(path: string | null | undefined): string | null {
  if (!path) {
    return null;
  }
  if (/^https?:\/\//i.test(path)) {
    return path;
  }
  if (path.startsWith('/') && API_BASE_URL) {
    return `${API_BASE_URL}${path}`;
  }
  return null;
}
