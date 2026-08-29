import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { get } from '@/shared/api/http'
import type { CustomerOptionItemResponse, CustomerSiteOptionItemResponse, CustomerOptionResponse } from '../types/customerApiTypes'


export const useCustomerMasterStore = defineStore('customer-master', () => {
  const loaded = ref(false)
  const loading = ref(false)

  const customers = ref<CustomerOptionItemResponse[]>([])
  const sites = ref<CustomerSiteOptionItemResponse[]>([])

  const load = async (force = false) => {
    if ((!force && loaded.value) || loading.value) return

    loading.value = true

    try {
      const response = await get<CustomerOptionResponse>(
        '/api/customers/options',
      )

      customers.value = response.customers
      sites.value = response.sites
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  /**
   * 顧客・現場の更新後に、日報と翌日準備が使う選択肢を再同期する。
   */
  const refresh = async () => {
    await load(true)
  }

  const customerOptions = computed(() =>
    customers.value.map((customer) => ({
      title: customer.name,
      value: customer.id,
    })),
  )

  const siteOptions = (customerId: number | null) => {
    if (customerId == null) return []

    return sites.value
      .filter((site) => site.customerId === customerId)
      .map((site) => ({
        title: site.name,
        value: site.id,
      }))
  }

  const findCustomer = (customerId: number | null) => {
    if (customerId == null) return undefined
    return customers.value.find((customer) => customer.id === customerId)
  }

  const findSite = (siteId: number | null) => {
    if (siteId == null) return undefined
    return sites.value.find((site) => site.id === siteId)
  }

  return {
    loaded,
    loading,
    customers,
    sites,
    customerOptions,
    siteOptions,
    findCustomer,
    findSite,
    load,
    refresh,
  }
})
