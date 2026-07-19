<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { z } from 'zod'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import TabbedForm from '@/shared/components/form/tabbed_form/TabbedForm.vue'
import DeductionDetailTable from '@/features/master/deduction/components/DeductionDetailTable.vue'
import type { DeductionMaster } from '@/features/master/deduction/types/deductionTypes'
import type { DeductionDetailResponse } from '@/features/master/deduction/types/deductionApiTypes'
import { useDeductionFormFields } from '@/features/master/deduction/composables/useDeductionFormFields'

const props = defineProps<{
  modelValue: boolean
  deduction: DeductionMaster | null
  detailResponse: DeductionDetailResponse | null
  isCreateMode: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', value: DeductionMaster): void
  (e: 'delete', id: number): void
}>()

const dialogModel = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const activeTab = ref<'basic' | 'details'>('basic')

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
})

const hasDetailTab = computed(() => form.detailViewType !== 'NONE')

const pageTabs = computed(() => {
  const tabs: { label: string; value: 'basic' | 'details' }[] = [
    { label: '基本情報', value: 'basic' },
  ]

  if (hasDetailTab.value) {
    tabs.push({ label: '詳細情報', value: 'details' })
  }

  return tabs
})

watch(
  () => props.modelValue,
  opened => {
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
      })
      return
    }

    Object.assign(form, props.deduction)
  },
)

watch(
  () => form.detailViewType,
  value => {
    if (value === 'NONE' && activeTab.value === 'details') {
      activeTab.value = 'basic'
    }
  },
)

const { tabs: formTabs, fields } = useDeductionFormFields()

const schema = z.object({
  code: z.string().trim().min(1, '控除コードは必須です'),
  name: z.string().trim().min(1, '控除名は必須です'),
}).passthrough()

function handleClose() {
  dialogModel.value = false
}

function handleSave() {
  emit('save', { ...form })
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
        <TabLayout v-model="activeTab" :tabs="pageTabs">
          <template #default="{ active }">
            <div v-if="active === 'basic'">
              <FormLayout v-model="form" :schema="schema">
                <TabbedForm
                  v-model="form"
                  :tabs="[...formTabs]"
                  :fields="fields"
                />
              </FormLayout>
            </div>

            <div
              v-else-if="active === 'details' && hasDetailTab"
            >
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
        <v-btn
          v-if="!isCreateMode"
          color="error"
          variant="text"
          @click="handleDelete"
        >
          控除削除
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