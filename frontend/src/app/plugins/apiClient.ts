import type { paths } from '@/shared/types/generated'
import { fetchWithAuthSession } from '@/shared/auth/session/authSession'
import createClient from 'openapi-fetch'

export const apiClient = createClient<paths>({
  baseUrl: import.meta.env.VITE_API_BASE_URL,
  fetch: fetchWithAuthSession,
})
