import { computed, ref } from 'vue'

import type { CustomerTransactionReviewRow } from '@/features/master/customer/components/CustomerTransactionReviewDialog.vue'

export const useCustomerTransactionReview = () => {
  const dialog = ref(false)

  const transactions = computed<CustomerTransactionReviewRow[]>(() => [])

  const open = () => {
    dialog.value = true
  }

  const close = () => {
    dialog.value = false
  }

  return {
    dialog,
    transactions,
    open,
    close,
  }
}