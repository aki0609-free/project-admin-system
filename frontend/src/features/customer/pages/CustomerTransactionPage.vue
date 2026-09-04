<script setup lang="ts">
import { computed, ref } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'
import PaymentConfirmDialog from '../components/PaymentConfirmDialog.vue'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import { useCustomersQuery } from '../api/useCustomersQuery'
import { useCustomerTransactionsQuery } from '../api/useCustomerTransactionQuery'
import { useConfirmCustomerPaymentMutation } from '../api/useConfirmCustomerPaymentMutation'
import {
  toCustomerTransaction,
  toCustomerPaymentConfirmRequest,
} from '../mapper/customerTransactionMapper'
import { toCustomerListItem } from '../mapper/customerMapper'
import { useCustomerTransactionColumns } from '../composables/useCustomerTransactionColumns'
import type { CustomerTransaction, CustomerPaymentConfirmPayload } from '../types/customerTypes'

const selectedCustomerId = ref<number | null>(null)
const paymentDialog = ref(false)
const selectedTransaction = ref<CustomerTransaction | null>(null)

const customersQuery = useCustomersQuery()
const transactionsQuery = useCustomerTransactionsQuery(selectedCustomerId)
const confirmPaymentMutation = useConfirmCustomerPaymentMutation()

const { columns } = useCustomerTransactionColumns()

const customers = computed(() => customersQuery.customers.value.map(toCustomerListItem))

const customerOptions = computed(() => [
  { title: 'すべて', value: null },
  ...customers.value.map((customer) => ({
    title: customer.name,
    value: customer.id,
  })),
])

const customerNameMap = computed(() =>
  Object.fromEntries(customers.value.map((customer) => [customer.id, customer.name])),
)

const items = computed<CustomerTransaction[]>(() =>
  transactionsQuery.transactions.value.map((response) => ({
    ...toCustomerTransaction(response),
    customerName: customerNameMap.value[response.customerId] ?? '',
  })),
)

const filterRules = computed(() => createSimpleTableFilterRules<CustomerTransaction>(columns.value))

const toolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '再読込',
    intent: 'utility',
    onClick: () => transactionsQuery.refetch(),
  },
])

function handleRowClick(row: CustomerTransaction) {
  selectedTransaction.value = row
  paymentDialog.value = true
}

async function handleConfirmPayment(payload: CustomerPaymentConfirmPayload) {
  if (!selectedTransaction.value) return

  await confirmPaymentMutation.mutateAsync({
    customerId: selectedTransaction.value.customerId,
    transactionId: selectedTransaction.value.id,
    body: toCustomerPaymentConfirmRequest(payload),
  })

  selectedTransaction.value = null
}
</script>

<template>
  <ListDetailPageLayout
    title="取引管理"
    description="顧客別の請求・入金状況を確認し、入金を確定します。"
    :right-toolbar-items="toolbarItems"
  >
    <div class="d-flex flex-column ga-4">
      <v-card variant="outlined" rounded="lg" class="customer-transaction-controls">
        <v-select
          v-model="selectedCustomerId"
          :items="customerOptions"
          item-title="title"
          item-value="value"
          label="顧客"
          variant="outlined"
          density="compact"
          hide-details
          clearable
          style="max-width: 360px"
        />
      </v-card>

      <v-alert v-if="transactionsQuery.isError.value" type="error" variant="tonal">
        取引情報の取得に失敗しました。
      </v-alert>

      <SimpleTable
        table-key="customer-transactions"
        item-key="id"
        :items="items"
        :columns="columns"
        :filter-rules="filterRules"
        :enable-row-click="true"
        @row-click="handleRowClick"
      />

    </div>

    <template #dialogs>
      <PaymentConfirmDialog
          v-model="paymentDialog"
          :transaction="selectedTransaction"
          @confirm="handleConfirmPayment"
        />
    </template>
  </ListDetailPageLayout>
</template>

<style scoped>
.customer-transaction-controls {
  display: grid;
  grid-template-columns: minmax(240px, 360px) minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #ffffff;
}

@media (max-width: 720px) {
  .customer-transaction-controls {
    grid-template-columns: 1fr;
  }
}
</style>
