<script setup lang="ts">
import { computed, reactive } from 'vue'

import SearchPanel from '@/shared/components/search/SearchPanel.vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { SearchPanelFieldDef } from '@/shared/components/search/types/searchPanelTypes'
import type { CustomerTransaction } from '@/features/master/customer/types/customerTypes'

export type CustomerTransactionReviewRow = CustomerTransaction & {
  customerName: string
}

type Filter = {
  targetMonth: string
  customerName: string
  paymentStatus: string
}

const props = defineProps<{
  modelValue: boolean
  transactions: CustomerTransactionReviewRow[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const filter = reactive<Filter>({
  targetMonth: '',
  customerName: '',
  paymentStatus: '',
})

const fields: SearchPanelFieldDef<Filter>[] = [
  { key: 'targetMonth', label: '対象年月', type: 'month', md: 3 },
  { key: 'customerName', label: '顧客名', type: 'text', md: 4 },
  {
    key: 'paymentStatus',
    label: '入金状態',
    type: 'select',
    md: 3,
    options: [
      { title: 'すべて', value: '' },
      { title: '未入金', value: 'unpaid' },
      { title: '入金済', value: 'paid' },
    ],
  },
]

const columns: SimpleTableColumnDef<CustomerTransactionReviewRow>[] = [
  { title: '対象月', key: 'targetMonth', width: '120px' },
  { title: '顧客名', key: 'customerName', width: '220px' },
  { title: '請求額', key: 'billingAmount', width: '120px' },
  { title: '入金予定日', key: 'expectedPaymentDate', width: '140px' },
  { title: '入金日', key: 'confirmedPaymentDate', width: '140px' },
  { title: '入金額', key: 'paidAmount', width: '120px' },
  { title: '手数料', key: 'fee', width: '100px' },
  { title: '合計', key: 'totalAmount', width: '120px' },
  { title: '入金済', key: 'isPaid', width: '100px', formatter: value => value ? '済' : '未' },
  { title: '備考', key: 'note', width: '240px' },
]

const filteredTransactions = computed(() =>
  props.transactions.filter(row => {
    if (filter.targetMonth && row.targetMonth !== filter.targetMonth) return false

    if (filter.customerName) {
      const text = row.customerName.toLowerCase()
      if (!text.includes(filter.customerName.toLowerCase())) return false
    }

    if (filter.paymentStatus === 'paid' && !row.isPaid) return false
    if (filter.paymentStatus === 'unpaid' && row.isPaid) return false

    return true
  }),
)

const clearFilter = () => {
  filter.targetMonth = ''
  filter.customerName = ''
  filter.paymentStatus = ''
}
</script>

<template>
  <v-dialog
    :model-value="modelValue"
    max-width="1200"
    scrollable
    @update:model-value="emit('update:modelValue', $event)"
  >
    <v-card rounded="xl">
      <v-card-title class="font-weight-bold">
        取引確認
      </v-card-title>

      <v-divider />

      <v-card-text>
        <SearchPanel
          v-model="filter"
          :fields="fields"
          @clear="clearFilter"
        />

        <SimpleTable
          table-key="customer-transaction-review"
          item-key="id"
          :items="filteredTransactions"
          :columns="columns"
        />
      </v-card-text>

      <v-card-actions class="justify-end">
        <v-btn
          variant="text"
          @click="emit('update:modelValue', false)"
        >
          閉じる
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>