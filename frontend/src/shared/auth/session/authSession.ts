const ACCESS_TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'
const DEFAULT_TENANT_ID = 'default'

export const AUTH_SESSION_EXPIRED_EVENT = 'auth-session-expired'

type RefreshResponse = {
  accessToken?: string
  refreshToken?: string
}

let refreshPromise: Promise<string | null> | null = null
let expirationNotified = false

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

const resolveUrl = (value: string): string => {
  if (/^https?:\/\//i.test(value)) return value

  const path = value.startsWith('/') ? value : `/${value}`
  if (apiBaseUrl) return `${apiBaseUrl}${path}`

  return new URL(path, window.location.origin).toString()
}

const requestUrl = (input: RequestInfo | URL): string => {
  if (input instanceof Request) return input.url
  if (input instanceof URL) return input.toString()
  return resolveUrl(input)
}

export const isAuthEndpoint = (input: RequestInfo | URL): boolean => {
  const pathname = new URL(requestUrl(input), window.location.origin).pathname
  return pathname === '/auth/login' || pathname === '/auth/refresh'
}

export const getAccessToken = (): string | null =>
  localStorage.getItem(ACCESS_TOKEN_KEY)

export const getRefreshToken = (): string | null =>
  localStorage.getItem(REFRESH_TOKEN_KEY)

export const saveAuthTokens = (
  accessToken: string,
  refreshToken: string,
): void => {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  expirationNotified = false
}

export const clearStoredAuth = (): void => {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export const expireAuthSession = (): void => {
  clearStoredAuth()

  if (expirationNotified) return
  expirationNotified = true
  window.dispatchEvent(new CustomEvent(AUTH_SESSION_EXPIRED_EVENT))
}

const executeRefresh = async (): Promise<string | null> => {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    expireAuthSession()
    return null
  }

  try {
    const response = await fetch(resolveUrl('/auth/refresh'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Tenant-ID': DEFAULT_TENANT_ID,
      },
      body: JSON.stringify({ refreshToken }),
    })

    if (!response.ok) {
      expireAuthSession()
      return null
    }

    const data = (await response.json()) as RefreshResponse
    if (!data.accessToken || !data.refreshToken) {
      expireAuthSession()
      return null
    }

    saveAuthTokens(data.accessToken, data.refreshToken)
    return data.accessToken
  } catch {
    expireAuthSession()
    return null
  }
}

export const refreshAccessToken = (): Promise<string | null> => {
  if (!refreshPromise) {
    refreshPromise = executeRefresh().finally(() => {
      refreshPromise = null
    })
  }

  return refreshPromise
}

const createAuthenticatedRequest = (
  input: RequestInfo | URL,
  init?: RequestInit,
  accessToken: string | null = getAccessToken(),
): Request => {
  const resolvedInput =
    typeof input === 'string' ? resolveUrl(input) : input
  const request = new Request(resolvedInput, init)
  const headers = new Headers(request.headers)

  headers.set('X-Tenant-ID', DEFAULT_TENANT_ID)
  if (accessToken && !isAuthEndpoint(request)) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  } else {
    headers.delete('Authorization')
  }

  return new Request(request, { headers })
}

export const fetchWithAuthSession: typeof fetch = async (input, init) => {
  const request = createAuthenticatedRequest(input, init)
  const response = await fetch(request.clone())

  if (response.status !== 401 || isAuthEndpoint(request)) {
    return response
  }

  const refreshedAccessToken = await refreshAccessToken()
  if (!refreshedAccessToken) return response

  const retryRequest = createAuthenticatedRequest(
    request,
    undefined,
    refreshedAccessToken,
  )
  return fetch(retryRequest)
}
