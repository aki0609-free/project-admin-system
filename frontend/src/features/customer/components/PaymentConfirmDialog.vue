<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { z } from 'zod'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import { formatYearMonth } from '@/shared/utils/DateUtils'
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
  adjustmentAmount: 0,
  confirmedPaymentDate: new Date().toISOString().slice(0, 10),
  note: null,
})
const formLayoutRef = ref<{ validateAll: () => boolean } | null>(null)

const schema = z.object({
  paidAmount: z.number().nullable().refine(value => value == null || value >= 0, '入金額は0以上で入力してください'),
  fee: z.number().nullable().refine(value => value == null || value >= 0, '手数料は0以上で入力してください'),
  offsetAmount: z.number().nullable().refine(value => value == null || value >= 0, '相殺額は0以上で入力してください'),
  adjustmentAmount: z.number().nullable(),
  confirmedPaymentDate: z.string().nullable().refine(value => Boolean(value), '必須です'),
  note: z.string().nullable(),
})

const fields: GridFormFieldDef<CustomerPaymentConfirmPayload>[] = [
  { key: 'fee', label: '手数料', type: 'number', gridColumn: '1 / span 2' },
  { key: 'paidAmount', label: '入金額', type: 'number', gridColumn: '3 / span 2' },
  { key: 'offsetAmount', label: '相殺額', type: 'number', gridColumn: '1 / span 2' },
  { key: 'adjustmentAmount', label: 'その他調整額', type: 'number', gridColumn: '3 / span 2' },
  {
    key: 'confirmedPaymentDate',
    label: '入金確認日',
    type: 'date',
    gridColumn: '1 / span 2',
  },
  { key: 'note', label: '備考・調整理由', type: 'text', gridColumn: '1 / -1' },
]

const billingAmount = computed(() => props.transaction?.billingAmount ?? 0)

const collectedAmount = computed(
  () => (form.paidAmount ?? 0)
    + (form.fee ?? 0)
    + (form.offsetAmount ?? 0)
    + (form.adjustmentAmount ?? 0),
)

const remainingAmount = computed(() => billingAmount.value - collectedAmount.value)

const expectedStatus = computed(() => {
  if (collectedAmount.value <= 0) return '未入金'
  if (remainingAmount.value > 0) return '一部入金'
  if (remainingAmount.value === 0) return '入金済'
  return '過入金'
})

const adjustmentReasonError = computed(
  () => (form.adjustmentAmount ?? 0) !== 0 && !form.note?.trim(),
)

watch(
  () => props.transaction,
  (value) => {
    if (!value) return

    form.paidAmount = value.paidAmount ?? null
    form.fee = value.fee ?? 0
    form.offsetAmount = value.offsetAmount ?? 0
    form.adjustmentAmount = value.adjustmentAmount ?? 0
    form.confirmedPaymentDate = value.confirmedPaymentDate ?? new Date().toISOString().slice(0, 10)
    form.note = value.note ?? null
  },
  { immediate: true },
)

function handleClose() {
  dialogModel.value = false
}

function handleConfirm() {
  if (!formLayoutRef.value?.validateAll() || adjustmentReasonError.value) return
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
      <div>対象月：{{ formatYearMonth(transaction.targetMonth) }}</div>
      <div>請求金額：{{ billingAmount.toLocaleString() }}円</div>
    </div>

    <v-alert type="info" variant="tonal">
      <div>請求金額：{{ billingAmount.toLocaleString() }}円</div>
      <div>回収額：{{ collectedAmount.toLocaleString() }}円</div>
      <div>残高：{{ remainingAmount.toLocaleString() }}円</div>
      <div>判定予定：{{ expectedStatus }}</div>
    </v-alert>

    <v-alert v-if="adjustmentReasonError" type="error" variant="tonal">
      その他調整額を入力する場合は、備考へ調整理由を入力してください。
    </v-alert>

    <FormLayout ref="formLayoutRef" v-model="form" :schema="schema">
      <GridBasedForm v-model="form" :fields="fields" />
    </FormLayout>
  </AppDialog>
</template>
