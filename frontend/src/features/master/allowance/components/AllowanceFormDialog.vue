<script setup lang="ts">
import { computed, reactive, ref, toRef, watch } from 'vue'
import { z } from 'zod'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import TabbedForm from '@/shared/components/form/tabbed_form/TabbedForm.vue'
import type { AllowanceMaster } from '@/features/master/allowance/types/allowanceTypes'
import { useAllowanceFormFields } from '@/features/master/allowance/composables/useAllowanceFormFields'
import { usePayrollRuleOptionsQuery } from '@/features/master/payrollitem/api/usePayrollRuleOptionsQuery'

const props = defineProps<{
  modelValue: boolean
  allowance: AllowanceMaster | null
  isCreateMode: boolean
  canManage: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', value: AllowanceMaster): void
  (e: 'delete', id: number): void
}>()

const dialogModel = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const form = reactive<AllowanceMaster>({
  id: -1,
  code: '',
  name: '',
  allowanceType: 'COMPANY',
  calculationType: 'MANUAL',
  allowanceUnit: 'MONTHLY',
  detailViewType: 'NONE',
  ruleName: null,
  defaultAmount: null,
  allowManualInput: true,
  minAmount: null,
  maxAmount: null,
  taxable: true,
  showOnDailyStatement: false,
  showOnMonthlyStatement: true,
  displayOrder: null,
  enabled: true,
  note: '',
})

watch(
  () => props.modelValue,
  opened => {
    if (!opened) return

    if (!props.allowance) {
      Object.assign(form, {
        id: -1,
        code: '',
        name: '',
        allowanceType: 'COMPANY',
        calculationType: 'MANUAL',
        allowanceUnit: 'MONTHLY',
        detailViewType: 'NONE',
        ruleName: null,
        defaultAmount: null,
        allowManualInput: true,
        minAmount: null,
        maxAmount: null,
        taxable: true,
        showOnDailyStatement: false,
        showOnMonthlyStatement: true,
        displayOrder: null,
        enabled: true,
        note: '',
      })
      return
    }

    Object.assign(form, props.allowance)
  },
)

const saveError = ref('')
const canLoadRules = computed(() => props.modelValue && props.canManage)
const ruleOptionsQuery = usePayrollRuleOptionsQuery(canLoadRules, 'ALLOWANCE')
const { tabs: formTabs, fields } = useAllowanceFormFields({
  isCreateMode: toRef(props, 'isCreateMode'),
  canManage: toRef(props, 'canManage'),
  ruleOptions: ruleOptionsQuery.options,
})

const schema = z.object({
  code: z.string().trim().min(1, '手当コードは必須です'),
  name: z.string().trim().min(1, '手当名は必須です'),
}).passthrough()

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
    return '手当コードは英大文字で始まる50文字以内の英大文字・数字・_で入力してください。'
  }
  form.code = code
  if (!form.name.trim()) return '手当名は必須です。'
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
  if (!confirm(`手当「${form.name}」を削除しますか？`)) return
  emit('delete', form.id)
}
</script>

<template>
  <v-dialog v-model="dialogModel" max-width="1200">
    <v-card>
      <v-card-title>
        {{ isCreateMode ? '手当 新規登録' : '手当 編集' }}
      </v-card-title>

      <v-card-text>
        <v-alert v-if="saveError" type="error" variant="tonal" class="mb-4">
          {{ saveError }}
        </v-alert>
        <FormLayout v-model="form" :schema="schema">
          <TabbedForm
            v-model="form"
            :tabs="[...formTabs]"
            :fields="fields"
          />
        </FormLayout>
      </v-card-text>

      <v-card-actions>
        <v-btn
          v-if="canManage && !isCreateMode"
          color="error"
          variant="text"
          @click="handleDelete"
        >
          手当削除
        </v-btn>

        <v-spacer />

        <v-btn variant="text" @click="handleClose">
          キャンセル
        </v-btn>

        <v-btn v-if="canManage" color="primary" @click="handleSave">
          保存
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
