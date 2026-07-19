import type { paths } from '@/shared/types/generated'
import createClient from 'openapi-fetch'

export const apiClient = createClient<paths>({
  baseUrl: import.meta.env.VITE_API_BASE_URL,
})

apiClient.use({
  async onRequest({ request }) {
    const token = localStorage.getItem('accessToken')

    if (token) {
      request.headers.set('Authorization', `Bearer ${token}`)
    }

    return request
  },
})