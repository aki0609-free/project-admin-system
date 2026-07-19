<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { z } from 'zod'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import TabbedForm from '@/shared/components/form/tabbed_form/TabbedForm.vue'
import type { AllowanceMaster } from '@/features/master/allowance/types/allowanceTypes'
import { useAllowanceFormFields } from '@/features/master/allowance/composables/useAllowanceFormFields'

const props = defineProps<{
  modelValue: boolean
  allowance: AllowanceMaster | null
  isCreateMode: boolean
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

const { tabs: formTabs, fields } = useAllowanceFormFields()

const schema = z.object({
  code: z.string().trim().min(1, '手当コードは必須です'),
  name: z.string().trim().min(1, '手当名は必須です'),
}).passthrough()

function handleClose() {
  dialogModel.value = false
}

function handleSave() {
  emit('save', { ...form })
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
          v-if="!isCreateMode"
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

        <v-btn color="primary" @click="handleSave">
          保存
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>