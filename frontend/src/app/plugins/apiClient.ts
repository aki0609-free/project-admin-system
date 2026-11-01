import type { paths } from '@/shared/types/generated'
import createClient from 'openapi-fetch'

const baseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

type RefreshResponse = {
  accessToken?: string
  refreshToken?: string
}

let refreshPromise: Promise<string | null> | null = null

const clearAuthTokens = () => {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
}

const refreshAccessToken = async (): Promise<string | null> => {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return null

  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const response = await fetch(`${baseUrl}/auth/refresh`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ refreshToken }),
        })

        if (!response.ok) {
          clearAuthTokens()
          return null
        }

        const data = await response.json() as RefreshResponse
        if (!data.accessToken) {
          clearAuthTokens()
          return null
        }

        localStorage.setItem('accessToken', data.accessToken)
        if (data.refreshToken) {
          localStorage.setItem('refreshToken', data.refreshToken)
        }
        return data.accessToken
      } catch {
        clearAuthTokens()
        return null
      }
    })().finally(() => {
      refreshPromise = null
    })
  }

  return refreshPromise
}

const authenticatedFetch: typeof fetch = async (input, init) => {
  const request = input instanceof Request
    ? input
    : new Request(input, init)
  const retryRequest = request.clone()
  const response = await fetch(request)

  if (response.status !== 401 || request.url.includes('/auth/')) {
    return response
  }

  const accessToken = await refreshAccessToken()
  if (!accessToken) return response

  const headers = new Headers(retryRequest.headers)
  headers.set('Authorization', `Bearer ${accessToken}`)
  return fetch(new Request(retryRequest, { headers }))
}

export const apiClient = createClient<paths>({
  baseUrl,
  fetch: authenticatedFetch,
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
