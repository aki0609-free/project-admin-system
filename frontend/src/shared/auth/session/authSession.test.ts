import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  AUTH_SESSION_EXPIRED_EVENT,
  fetchWithAuthSession,
  getAccessToken,
  getRefreshToken,
  saveAuthTokens,
} from './authSession'

const requestPath = (input: RequestInfo | URL): string => {
  const url = input instanceof Request ? input.url : input.toString()
  return new URL(url, window.location.origin).pathname
}

describe('authSession', () => {
  beforeEach(() => {
    const values = new Map<string, string>()
    vi.stubGlobal('localStorage', {
      getItem: (key: string) => values.get(key) ?? null,
      setItem: (key: string, value: string) => values.set(key, value),
      removeItem: (key: string) => values.delete(key),
      clear: () => values.clear(),
      key: (index: number) => Array.from(values.keys())[index] ?? null,
      get length() {
        return values.size
      },
    } satisfies Storage)
    saveAuthTokens('expired-access-token', 'valid-refresh-token')
    vi.restoreAllMocks()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('refreshes once and retries the original request after a 401', async () => {
    const apiAuthorizationHeaders: Array<string | null> = []
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const path = requestPath(input)
      if (path === '/auth/refresh') {
        return new Response(
          JSON.stringify({
            accessToken: 'new-access-token',
            refreshToken: 'new-refresh-token',
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }

      const request = input as Request
      apiAuthorizationHeaders.push(request.headers.get('Authorization'))
      return new Response(null, {
        status: apiAuthorizationHeaders.length === 1 ? 401 : 200,
      })
    })
    vi.stubGlobal('fetch', fetchMock)

    const response = await fetchWithAuthSession('/api/customers')

    expect(response.status).toBe(200)
    expect(apiAuthorizationHeaders).toEqual([
      'Bearer expired-access-token',
      'Bearer new-access-token',
    ])
    expect(getAccessToken()).toBe('new-access-token')
    expect(getRefreshToken()).toBe('new-refresh-token')
  })

  it('shares a single refresh request between concurrent 401 responses', async () => {
    let refreshCalls = 0
    const apiCalls = new Map<string, number>()
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const path = requestPath(input)
      if (path === '/auth/refresh') {
        refreshCalls += 1
        return new Response(
          JSON.stringify({
            accessToken: 'shared-access-token',
            refreshToken: 'shared-refresh-token',
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }

      const count = (apiCalls.get(path) ?? 0) + 1
      apiCalls.set(path, count)
      return new Response(null, { status: count === 1 ? 401 : 200 })
    })
    vi.stubGlobal('fetch', fetchMock)

    const [first, second] = await Promise.all([
      fetchWithAuthSession('/api/customers/1'),
      fetchWithAuthSession('/api/customers/2'),
    ])

    expect(first.status).toBe(200)
    expect(second.status).toBe(200)
    expect(refreshCalls).toBe(1)
  })

  it('clears authentication and notifies once when refresh fails', async () => {
    const expiredListener = vi.fn()
    window.addEventListener(AUTH_SESSION_EXPIRED_EVENT, expiredListener)
    const fetchMock = vi.fn(async () => new Response(null, { status: 401 }))
    vi.stubGlobal('fetch', fetchMock)

    const [first, second] = await Promise.all([
      fetchWithAuthSession('/api/customers/1'),
      fetchWithAuthSession('/api/customers/2'),
    ])

    expect(first.status).toBe(401)
    expect(second.status).toBe(401)
    expect(getAccessToken()).toBeNull()
    expect(getRefreshToken()).toBeNull()
    expect(expiredListener).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledTimes(3)
    window.removeEventListener(AUTH_SESSION_EXPIRED_EVENT, expiredListener)
  })

  it('does not attempt refresh for a failed login request', async () => {
    const fetchMock = vi.fn(async () => new Response(null, { status: 401 }))
    vi.stubGlobal('fetch', fetchMock)

    const response = await fetchWithAuthSession('/auth/login', {
      method: 'POST',
    })

    expect(response.status).toBe(401)
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
})
