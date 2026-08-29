import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useCustomerMasterStore } from './useCustomerMasterStore'

const { getMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
}))

vi.mock('@/shared/api/http', () => ({
  get: getMock,
}))

describe('useCustomerMasterStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    getMock.mockReset()
  })

  it('通常loadはキャッシュし、refreshは顧客・現場候補を再取得する', async () => {
    getMock
      .mockResolvedValueOnce({
        customers: [{ id: 1, name: '更新前顧客' }],
        sites: [{
          id: 10,
          customerId: 1,
          name: '更新前現場',
          distanceFromCompanyKm: 5,
        }],
      })
      .mockResolvedValueOnce({
        customers: [{ id: 1, name: '更新後顧客' }],
        sites: [{
          id: 11,
          customerId: 1,
          name: '更新後現場',
          distanceFromCompanyKm: 8,
        }],
      })
    const store = useCustomerMasterStore()

    await store.load()
    await store.load()

    expect(getMock).toHaveBeenCalledTimes(1)
    expect(store.customerOptions).toEqual([
      { title: '更新前顧客', value: 1 },
    ])

    await store.refresh()

    expect(getMock).toHaveBeenCalledTimes(2)
    expect(store.customerOptions).toEqual([
      { title: '更新後顧客', value: 1 },
    ])
    expect(store.siteOptions(1)).toEqual([
      { title: '更新後現場', value: 11 },
    ])
  })
})
