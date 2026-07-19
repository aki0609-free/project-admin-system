import {
  computed,
  type MaybeRef,
  unref,
} from 'vue'

import type {
  SimpleTableColumnDef,
} from '@/shared/components/table/simple_table/types/item/types'

import type {
  CustomerSite,
  CustomerSiteBillingRate,
} from '../types/customerTypes'

import { formatCurrency } from '@/shared/utils/CurrencyUtils'

type SelectItem = {
  title: string
  value: string | number
}

const billingUnitItems: SelectItem[] = [
  {
    title: '日額',
    value: 'DAILY',
  },
  {
    title: '時間単価',
    value: 'HOURLY',
  },
  {
    title: '月額',
    value: 'MONTHLY',
  },
  {
    title: '固定額',
    value: 'FIXED',
  },
]

const billingUnitLabels:
  Record<string, string> = {
    DAILY: '日額',
    HOURLY: '時間単価',
    MONTHLY: '月額',
    FIXED: '固定額',
  }

function formatNullableCurrency(
  value: unknown,
): string {
  if (
    value == null
    || value === ''
    || Number.isNaN(Number(value))
  ) {
    return ''
  }

  return formatCurrency(Number(value))
}

export const useCustomerBillingRateColumns = (
  sites: MaybeRef<CustomerSite[]>,
) => {
  const siteItems = computed<SelectItem[]>(() =>
    unref(sites)
      .filter(site =>
        !site._isDeleted
        && site.id > 0,
      )
      .map(site => ({
        title: site.name,
        value: site.id,
      })),
  )

  const siteNameMap = computed(() =>
    new Map(
      unref(sites).map(site => [
        site.id,
        site.name,
      ]),
    ),
  )

  const columns = computed<
    SimpleTableColumnDef<CustomerSiteBillingRate>[]
  >(() => [
    {
      title: '削除',
      key: 'deleteSelected',
      width: '75px',
      type: 'checkbox',
      editable: true,
      filter: {
        type: 'checkbox',
      },
    },
    {
      title: '順番',
      key: 'displayOrder',
      width: '80px',
      type: 'number',
      editable: true,
      filter: {
        type: 'text',
      },
    },
    {
      title: '現場',
      key: 'customerSiteId',
      width: '200px',
      type: 'select',
      editable: true,
      items: siteItems.value,
      filter: {
        type: 'select',
        items: siteItems.value,
      },
      formatter: value =>
        siteNameMap.value.get(Number(value)) ?? '',
    },
    {
      title: '職種コード',
      key: 'jobCode',
      width: '130px',
      type: 'text',
      editable: true,
      filter: {
        type: 'text',
      },
    },
    {
      title: '職種名',
      key: 'jobName',
      width: '170px',
      type: 'text',
      editable: true,
      filter: {
        type: 'text',
      },
    },
    {
      title: '役職コード',
      key: 'siteRoleCode',
      width: '130px',
      type: 'text',
      editable: true,
      filter: {
        type: 'text',
      },
    },
    {
      title: '現場役職',
      key: 'siteRoleName',
      width: '150px',
      type: 'text',
      editable: true,
      filter: {
        type: 'text',
      },
    },
    {
      title: '単価区分',
      key: 'billingUnit',
      width: '125px',
      type: 'select',
      editable: true,
      items: billingUnitItems,
      filter: {
        type: 'select',
        items: billingUnitItems,
      },
      formatter: value =>
        billingUnitLabels[
          String(value ?? '')
        ] ?? '',
    },
    {
      title: '基準単価',
      key: 'baseUnitPrice',
      width: '130px',
      type: 'number',
      editable: true,
      filter: {
        type: 'text',
      },
      formatter: formatNullableCurrency,
    },
    {
      title: '残業単価',
      key: 'overtimeUnitPrice',
      width: '130px',
      type: 'number',
      editable: true,
      filter: {
        type: 'text',
      },
      formatter: formatNullableCurrency,
    },
    {
      title: '深夜単価',
      key: 'nightUnitPrice',
      width: '130px',
      type: 'number',
      editable: true,
      filter: {
        type: 'text',
      },
      formatter: formatNullableCurrency,
    },
    {
      title: '通勤単価',
      key: 'commuteUnitPrice',
      width: '130px',
      type: 'number',
      editable: true,
      filter: {
        type: 'text',
      },
      formatter: formatNullableCurrency,
    },
    {
      title: '適用開始日',
      key: 'effectiveFrom',
      width: '140px',
      type: 'date',
      editable: true,
      filter: {
        type: 'date',
      },
    },
    {
      title: '適用終了日',
      key: 'effectiveTo',
      width: '140px',
      type: 'date',
      editable: true,
      filter: {
        type: 'date',
      },
    },
    {
      title: '有効',
      key: 'activeFlag',
      width: '80px',
      type: 'checkbox',
      editable: true,
      filter: {
        type: 'checkbox',
      },
    },
    {
      title: '備考',
      key: 'note',
      width: '220px',
      type: 'text',
      editable: true,
      filter: {
        type: 'text',
      },
    },
  ])

  return {
    columns,
    siteItems,
  }
}