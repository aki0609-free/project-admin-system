import type { Ref } from 'vue'
import type { CustomerTransaction } from '@/features/master/customer/types/customerTypes'

export type ConfirmPaymentInput = {
  paidAmount: number | null
  fee: number | null
  confirmedPaymentDate: string | null
}

function addMonth(yearMonth: string): string {
  const [year, month] = yearMonth.split('-').map(Number)
  if (!year || !month) return yearMonth

  const date = new Date(year, month - 1, 1)
  date.setMonth(date.getMonth() + 1)

  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

export const useCustomerTransactionActions = (
  transactions: Ref<CustomerTransaction[]>,
) => {
  const confirmPayment = (
    id: number,
    input: ConfirmPaymentInput,
  ) => {
    const current = transactions.value.find(item => item.id === id)
    if (!current) return

    const paidAmount = input.paidAmount ?? 0
    const fee = input.fee ?? 0
    const billingAmount = current.billingAmount ?? 0

    const updatedRow: CustomerTransaction = {
      ...current,
      paidAmount,
      fee,
      confirmedPaymentDate: input.confirmedPaymentDate,
      totalAmount: paidAmount + fee,
      isPaid: paidAmount >= billingAmount,
      _isUpdated: current._isNew ? current._isUpdated : true,
    }

    let nextRows = transactions.value.map(item =>
      item.id === id ? updatedRow : item,
    )

    if (updatedRow.isPaid) {
      nextRows = addNextMonthRow(nextRows, updatedRow)
    }

    transactions.value = nextRows
  }

  const addNextMonthRow = (
    rows: CustomerTransaction[],
    current: CustomerTransaction,
  ): CustomerTransaction[] => {
    const nextMonth = addMonth(current.targetMonth)

    const exists = rows.some(
      item =>
        item.customerId === current.customerId &&
        item.targetMonth === nextMonth &&
        !item._isDeleted,
    )

    if (exists) return rows

    return [
      ...rows,
      {
        id: Date.now(),
        customerId: current.customerId,
        targetMonth: nextMonth,
        closingDay: current.closingDay,
        paymentDay: current.paymentDay,
        billingAmount: null,
        expectedPaymentDate: null,
        confirmedPaymentDate: null,
        paidAmount: null,
        fee: null,
        totalAmount: null,
        isPaid: false,
        note: '',
        deleteSelected: false,
        _isNew: true,
        _isUpdated: false,
        _isDeleted: false,
      },
    ]
  }

  return {
    confirmPayment,
  }
}