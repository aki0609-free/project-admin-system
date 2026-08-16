<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import type { CustomerPaymentConfirmPayload, CustomerTransaction } from '../types/customerTypes'

const props = defineProps<{
  modelValue: boolean
  transaction: CustomerTransaction | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm', value: CustomerPaymentConfirmPayload): void
}>()

const dialogModel = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const form = reactive<CustomerPaymentConfirmPayload>({
  paidAmount: null,
  fee: 0,
  offsetAmount: 0,
  confirmedPaymentDate: new Date().toISOString().slice(0, 10),
  note: null,
})

const billingAmount = computed(() => props.transaction?.billingAmount ?? 0)

const collectedAmount = computed(
  () => (form.paidAmount ?? 0) + (form.fee ?? 0) + (form.offsetAmount ?? 0),
)

const remainingAmount = computed(() => billingAmount.value - collectedAmount.value)

const expectedStatus = computed(() => {
  if (collectedAmount.value <= 0) return '未入金'
  if (remainingAmount.value > 0) return '一部入金'
  if (remainingAmount.value === 0) return '入金済'
  return '過入金'
})

watch(
  () => props.transaction,
  (value) => {
    if (!value) return

    form.paidAmount = value.paidAmount ?? null
    form.fee = value.fee ?? 0
    form.offsetAmount = value.offsetAmount ?? 0
    form.confirmedPaymentDate = value.confirmedPaymentDate ?? new Date().toISOString().slice(0, 10)
    form.note = value.note ?? null
  },
  { immediate: true },
)

function handleClose() {
  dialogModel.value = false
}

function handleConfirm() {
  emit('confirm', { ...form })
  dialogModel.value = false
}

const rightFooterItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: 'キャンセル',
    intent: 'utility',
    onClick: handleClose,
  },
  {
    type: 'button',
    label: '入金確定',
    color: 'primary',
    intent: 'primary',
    onClick: handleConfirm,
  },
])
</script>

<template>
  <AppDialog
    v-model="dialogModel"
    title="入金確認"
    size="sm"
    :max-width="560"
    body-layout="stack"
    :right-footer-items="rightFooterItems"
  >
    <div v-if="transaction">
      <div>対象月：{{ transaction.targetMonth }}</div>
      <div>請求金額：{{ billingAmount.toLocaleString() }}円</div>
    </div>

    <v-alert type="info" variant="tonal">
      <div>請求金額：{{ billingAmount.toLocaleString() }}円</div>
      <div>回収額：{{ collectedAmount.toLocaleString() }}円</div>
      <div>残高：{{ remainingAmount.toLocaleString() }}円</div>
      <div>判定予定：{{ expectedStatus }}</div>
    </v-alert>

    <v-text-field v-model.number="form.fee" label="手数料" type="number" density="compact" />

    <v-text-field v-model.number="form.paidAmount" label="入金額" type="number" density="compact" />

    <v-text-field
      v-model.number="form.offsetAmount"
      label="相殺額"
      type="number"
      density="compact"
    />

    <v-text-field
      v-model="form.confirmedPaymentDate"
      label="入金確認日"
      type="date"
      density="compact"
    />

    <v-text-field v-model="form.note" label="備考" density="compact" />
  </AppDialog>
</template>
