import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { CustomerTransaction } from '../types/customerTypes'
import { formatCurrency } from '@/shared/utils/CurrencyUtils'
import { formatYearMonthDay } from '@/shared/utils/DateUtils'
import { formatDayRule } from '@/shared/utils/DayRuleUtils'
import type { DayRule } from '@/shared/types/dayRuleTypes'

const paymentStatusLabelMap: Record<string, string> = {
  UNPAID: '未入金',
  PARTIAL: '一部入金',
  PAID: '入金済',
  OVERPAID: '過入金',
  CANCELED: '請求取消',
  WRITE_OFF: '貸倒',
}

const formatCurrencyNullable = (value: unknown): string =>
  typeof value === 'number' ? formatCurrency(value) : ''

const formatDateNullable = (value: unknown): string =>
  typeof value === 'string' ? formatYearMonthDay(value) : ''

const formatDayRuleNullable = (value: unknown): string =>
  value ? formatDayRule(value as DayRule) : ''

const formatPaymentStatus = (value: unknown): string =>
  paymentStatusLabelMap[String(value ?? '')] ?? ''

export const useCustomerTransactionColumns = () => {
  const columns = computed<SimpleTableColumnDef<CustomerTransaction>[]>(() => [
    {
      title: '顧客ID',
      key: 'customerId',
      width: '100px',
      filter: { type: 'text' },
    },
    {
      title: '対象月',
      key: 'targetMonth',
      width: '120px',
      filter: { type: 'text' },
    },
    {
      title: '締日',
      key: 'closingDayRule',
      width: '180px',
      filter: { type: 'text' },
      formatter: formatDayRuleNullable,
    },
    {
      title: '支払日',
      key: 'paymentDayRule',
      width: '180px',
      filter: { type: 'text' },
      formatter: formatDayRuleNullable,
    },
    {
      title: '取引金額',
      key: 'billingAmount',
      width: '140px',
      filter: { type: 'text' },
      formatter: formatCurrencyNullable,
    },
    {
      title: '手数料',
      key: 'fee',
      width: '120px',
      filter: { type: 'text' },
      formatter: formatCurrencyNullable,
    },
    {
      title: '請求合計',
      key: 'receivableAmount',
      width: '140px',
      filter: { type: 'text' },
      formatter: formatCurrencyNullable,
    },
    {
      title: '入金予定日',
      key: 'expectedPaymentDate',
      width: '140px',
      filter: { type: 'date' },
      formatter: formatDateNullable,
    },
    {
      title: '入金確認日',
      key: 'confirmedPaymentDate',
      width: '140px',
      filter: { type: 'date' },
      formatter: formatDateNullable,
    },
    {
      title: '入金額',
      key: 'paidAmount',
      width: '140px',
      filter: { type: 'text' },
      formatter: formatCurrencyNullable,
    },
    {
      title: '相殺額',
      key: 'offsetAmount',
      width: '120px',
      filter: { type: 'text' },
      formatter: formatCurrencyNullable,
    },
    {
      title: '入金合計',
      key: 'totalAmount',
      width: '140px',
      filter: { type: 'text' },
      formatter: formatCurrencyNullable,
    },
    {
      title: '入金状態',
      key: 'paymentStatus',
      width: '120px',
      filter: { type: 'text' },
      formatter: formatPaymentStatus,
    },
    {
      title: '備考',
      key: 'note',
      width: '220px',
      filter: { type: 'text' },
    },
  ])

  return { columns }
}