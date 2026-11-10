import axios, { AxiosHeaders } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { saveAuthTokens } from '@/shared/auth/session/authSession'
import axiosApiClient from './axiosApiClient'

const originalAdapter = axiosApiClient.defaults.adapter

describe('axiosApiClient session recovery', () => {
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
  })

  afterEach(() => {
    axiosApiClient.defaults.adapter = originalAdapter
    vi.unstubAllGlobals()
  })

  it('retries one axios request with the refreshed access token', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        new Response(
          JSON.stringify({
            accessToken: 'new-access-token',
            refreshToken: 'new-refresh-token',
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    )

    const authorizationHeaders: Array<string | undefined> = []
    axiosApiClient.defaults.adapter = async (config) => {
      authorizationHeaders.push(config.headers.get('Authorization')?.toString())

      if (authorizationHeaders.length === 1) {
        throw new axios.AxiosError(
          'Unauthorized',
          'ERR_BAD_REQUEST',
          config,
          undefined,
          {
            data: {},
            status: 401,
            statusText: 'Unauthorized',
            headers: new AxiosHeaders(),
            config,
          },
        )
      }

      return {
        data: { ok: true },
        status: 200,
        statusText: 'OK',
        headers: new AxiosHeaders(),
        config,
      }
    }

    const response = await axiosApiClient.get('/api/customers')

    expect(response.data).toEqual({ ok: true })
    expect(authorizationHeaders).toEqual([
      'Bearer expired-access-token',
      'Bearer new-access-token',
    ])
  })
})
