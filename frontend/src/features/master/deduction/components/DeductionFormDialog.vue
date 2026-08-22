<script setup lang="ts">
import { computed, reactive, ref, toRef, watch } from 'vue'
import { z } from 'zod'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import TabbedForm from '@/shared/components/form/tabbed_form/TabbedForm.vue'
import DeductionDetailTable from '@/features/master/deduction/components/DeductionDetailTable.vue'
import type { DeductionMaster } from '@/features/master/deduction/types/deductionTypes'
import type { DeductionDetailResponse } from '@/features/master/deduction/types/deductionApiTypes'
import { useDeductionFormFields } from '@/features/master/deduction/composables/useDeductionFormFields'
import { usePayrollRuleOptionsQuery } from '@/features/master/payrollitem/api/usePayrollRuleOptionsQuery'
import PayrollItemPolicyEditor from '@/features/master/payrollitem/components/PayrollItemPolicyEditor.vue'
import { createDefaultPayrollItemPolicy } from '@/features/master/payrollitem/types/payrollItemPolicyTypes'

const props = defineProps<{
  modelValue: boolean
  deduction: DeductionMaster | null
  detailResponse: DeductionDetailResponse | null
  isCreateMode: boolean
  canManage: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', value: DeductionMaster): void
  (e: 'delete', id: number): void
}>()

const dialogModel = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const activeTab = ref<'basic' | 'application' | 'details'>('basic')

const form = reactive<DeductionMaster>({
  id: -1,
  code: '',
  name: '',
  deductionType: 'COMPANY',
  calculationType: 'MANUAL',
  deductionUnit: 'MONTHLY',
  detailViewType: 'NONE',
  ruleName: null,
  defaultAmount: null,
  allowManualInput: true,
  minAmount: null,
  maxAmount: null,
  showOnDailyStatement: false,
  showOnMonthlyStatement: true,
  carryToMonthlySettlement: false,
  displayOrder: null,
  enabled: true,
  note: '',
  policy: createDefaultPayrollItemPolicy(),
})

const hasDetailTab = computed(() => form.detailViewType !== 'NONE')

const pageTabs = computed(() => {
  const tabs: { label: string; value: 'basic' | 'application' | 'details' }[] = [
    { label: '基本情報', value: 'basic' },
    { label: '適用・連携設定', value: 'application' },
  ]

  if (hasDetailTab.value) {
    tabs.push({ label: '詳細情報', value: 'details' })
  }

  return tabs
})

watch(
  () => props.modelValue,
  (opened) => {
    if (!opened) return

    activeTab.value = 'basic'

    if (!props.deduction) {
      Object.assign(form, {
        id: -1,
        code: '',
        name: '',
        deductionType: 'COMPANY',
        calculationType: 'MANUAL',
        deductionUnit: 'MONTHLY',
        detailViewType: 'NONE',
        ruleName: null,
        defaultAmount: null,
        allowManualInput: true,
        minAmount: null,
        maxAmount: null,
        showOnDailyStatement: false,
        showOnMonthlyStatement: true,
        carryToMonthlySettlement: false,
        displayOrder: null,
        enabled: true,
        note: '',
        policy: createDefaultPayrollItemPolicy(),
      })
      return
    }

    Object.assign(form, props.deduction)
  },
)

watch(
  [
    () => props.modelValue,
    () => props.detailResponse,
    () => props.deduction?.id,
    () => props.isCreateMode,
  ],
  ([opened, detail, deductionId, createMode]) => {
    if (!opened || !detail || createMode || detail.id !== deductionId) return
    form.policy = detail.policy
      ? structuredClone(detail.policy)
      : createDefaultPayrollItemPolicy()
  },
  { immediate: true },
)

watch(
  () => form.detailViewType,
  (value) => {
    if (value === 'NONE' && activeTab.value === 'details') {
      activeTab.value = 'basic'
    }
  },
)

const saveError = ref('')
const canLoadRules = computed(() => props.modelValue && props.canManage)
const ruleOptionsQuery = usePayrollRuleOptionsQuery(canLoadRules, 'DEDUCTION')
const { tabs: formTabs, fields } = useDeductionFormFields({
  isCreateMode: toRef(props, 'isCreateMode'),
  canManage: toRef(props, 'canManage'),
  ruleOptions: ruleOptionsQuery.options,
})

const schema = z
  .object({
    code: z.string().trim().min(1, '控除コードは必須です'),
    name: z.string().trim().min(1, '控除名は必須です'),
  })
  .passthrough()

function handleClose() {
  dialogModel.value = false
}

function handleSave() {
  saveError.value = validateForm()
  if (saveError.value) return
  emit('save', { ...form })
}

function validateForm(): string {
  const code = form.code.trim().toUpperCase()
  if (!/^[A-Z][A-Z0-9_]{0,49}$/.test(code)) {
    return '控除コードは英大文字で始まる50文字以内の英大文字・数字・_で入力してください。'
  }
  form.code = code
  if (!form.name.trim()) return '控除名は必須です。'
  if (form.calculationType === 'AUTO' && !form.ruleName) {
    return '自動計算ではRuleを選択してください。'
  }
  if (form.calculationType === 'FIXED' && form.defaultAmount == null) {
    return '固定計算では固定金額を入力してください。'
  }
  if (form.calculationType === 'MANUAL' && !form.allowManualInput) {
    return '手入力計算では手入力許可を有効にしてください。'
  }
  if (form.minAmount != null && form.maxAmount != null && form.minAmount > form.maxAmount) {
    return '下限金額は上限金額以下にしてください。'
  }
  return ''
}

function handleDelete() {
  if (props.isCreateMode) return
  if (!confirm(`控除「${form.name}」を削除しますか？`)) return
  emit('delete', form.id)
}
</script>

<template>
  <v-dialog v-model="dialogModel" max-width="1400">
    <v-card>
      <v-card-title>
        {{ isCreateMode ? '控除 新規登録' : '控除 編集' }}
      </v-card-title>

      <v-card-text>
        <v-alert v-if="saveError" type="error" variant="tonal" class="mb-4">
          {{ saveError }}
        </v-alert>
        <TabLayout v-model="activeTab" :tabs="pageTabs">
          <template #default="{ active }">
            <div v-if="active === 'basic'">
              <FormLayout v-model="form" :schema="schema">
                <TabbedForm v-model="form" :tabs="[...formTabs]" :fields="fields" />
              </FormLayout>
            </div>

            <PayrollItemPolicyEditor
              v-else-if="active === 'application'"
              v-model="form.policy"
              :can-manage="canManage"
            />

            <div v-else-if="active === 'details' && hasDetailTab">
              <DeductionDetailTable
                :deduction-id="form.id"
                :detail-view-type="form.detailViewType"
                :detail-response="detailResponse"
              />
            </div>
          </template>
        </TabLayout>
      </v-card-text>

      <v-card-actions>
        <v-btn v-if="canManage && !isCreateMode" color="error" variant="text" @click="handleDelete">
          控除削除
        </v-btn>

        <v-spacer />

        <v-btn variant="text" @click="handleClose"> キャンセル </v-btn>

        <v-btn v-if="canManage" color="primary" @click="handleSave"> 保存 </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
