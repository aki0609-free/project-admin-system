<script setup lang="ts">
import { computed, ref, toRef, watch } from 'vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import FormGridTab from '@/toolbox/tab/FormGridTab.vue'
import type { EmployeeDetailResponse, EmployeeResignRequest } from '../types/employeeApiTypes'
import type { EmployeeForm } from '../types/employeeFormTypes'
import { useEmployeeEditDialog } from '../composables/useEmployeeEditDialog'
import EmployeeResignDialog from './EmployeeResignDialog.vue'
import { useEmployeeResignationChecklistQuery } from '../api/useEmployeeResignationChecklistQuery'
import { useResignEmployeeMutation } from '../api/useResignEmployeeMutation'
import { useCancelEmployeeResignationMutation } from '../api/useCancelEmployeeResignationMutation'
import { useEmployeePayrollItemSettingCatalogQuery } from '../api/useEmployeePayrollItemSettingCatalogQuery'
import PayrollItemTransactionPanel from './PayrollItemTransactionPanel.vue'

const props = defineProps<{
  modelValue: boolean
  employee: EmployeeDetailResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save' | 'delete', value: EmployeeForm): void
  (e: 'resigned'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const resignationChecklistQuery = useEmployeeResignationChecklistQuery()
const resignMutation = useResignEmployeeMutation()
const cancelResignationMutation = useCancelEmployeeResignationMutation()
const activePayrollItemCode = ref('')
const payrollItemCatalogQuery = useEmployeePayrollItemSettingCatalogQuery()

const {
  activeTab,
  formModel,
  isEdit,
  tabs,
  basicFields,
  payrollFields,
  contractFields,
  basicSchema,
  payrollSchema,
  contractSchema,
  leftFooterItems,
  rightFooterItems,
  resignDialogVisible,
} = useEmployeeEditDialog(
  visible,
  toRef(props, 'employee'),
  (form) => emit('save', form),
  (form) => emit('delete', form),
  async (employeeId) => {
    const confirmed = window.confirm('退職を取り消して在籍状態へ戻しますか？')
    if (!confirmed) return
    await cancelResignationMutation.mutateAsync(employeeId)
    visible.value = false
    emit('resigned')
  },
)

const handleResign = async (request: EmployeeResignRequest) => {
  if (!formModel.id) {
    return
  }

  await resignMutation.mutateAsync({
    id: formModel.id,
    request,
  })

  resignDialogVisible.value = false
  visible.value = false
  emit('resigned')
}

watch(
  () => formModel.payrollItemSettings,
  (items) => {
    if (items.length && !items.some((item) => item.targetCode === activePayrollItemCode.value)) {
      const firstItem = items[0]
      if (firstItem) activePayrollItemCode.value = firstItem.targetCode
    }
  },
  { immediate: true, deep: true },
)

watch(
  [visible, () => props.employee, payrollItemCatalogQuery.settings],
  ([opened, currentEmployee, catalog]) => {
    if (!opened || currentEmployee || !catalog.length) return
    formModel.payrollItemSettings = catalog.map((item) => ({
      ...item,
      effectiveFrom: item.effectiveFrom ?? '',
      effectiveTo: item.effectiveTo ?? '',
      parameters:
        item.targetCode === 'DORMITORY_FEE'
          ? {
              ...item.parameters,
              dormitoryType: item.parameters.dormitoryType ?? '',
              collectionMode: item.parameters.collectionMode ?? 'DAILY',
            }
          : { ...item.parameters },
    }))
  },
  { immediate: true },
)

const activePayrollItem = computed(() =>
  formModel.payrollItemSettings.find((item) => item.targetCode === activePayrollItemCode.value),
)
</script>

<template>
  <AppDialog
    v-model="visible"
    :title="isEdit ? '従業員情報編集' : '従業員情報新規作成'"
    size="xl"
    :left-footer-items="leftFooterItems"
    :right-footer-items="rightFooterItems"
  >
    <TabLayout v-model="activeTab" :tabs="tabs">
      <template #default="{ active }">
        <FormGridTab
          v-if="active === 'basic'"
          v-model="formModel"
          :schema="basicSchema"
          :fields="basicFields"
        />

        <div v-else-if="active === 'payrollItems'" class="payroll-item-settings">
          <v-tabs v-model="activePayrollItemCode" color="primary">
            <v-tab
              v-for="item in formModel.payrollItemSettings"
              :key="item.targetCode"
              :value="item.targetCode"
            >
              {{ item.displayName }}
            </v-tab>
          </v-tabs>
          <v-card v-if="activePayrollItem" variant="outlined" class="pa-5 mt-4">
            <v-switch
              v-model="activePayrollItem.enabled"
              label="この項目を適用する"
              color="primary"
            />
            <template v-if="activePayrollItem.enabled">
              <v-select
                v-if="activePayrollItem.targetCode === 'DORMITORY_FEE'"
                v-model="activePayrollItem.parameters.dormitoryType"
                label="寮タイプ"
                :items="[
                  { title: '一人部屋', value: 'SINGLE_ROOM' },
                  { title: '複数人部屋', value: 'SHARED_ROOM' },
                ]"
                variant="outlined"
              />
              <v-select
                v-if="activePayrollItem.targetCode === 'DORMITORY_FEE'"
                v-model="activePayrollItem.parameters.collectionMode"
                label="徴収方式"
                :items="[
                  { title: '日報で日次徴収', value: 'DAILY' },
                  { title: '月1回の一括徴収', value: 'MONTHLY' },
                ]"
                variant="outlined"
              />
              <div v-if="activePayrollItem.balanceTracked" class="balance-grid">
                <v-text-field
                  :model-value="activePayrollItem.effectiveFrom"
                  label="適用開始日（自動）"
                  readonly
                  variant="outlined"
                />
                <v-text-field
                  :model-value="activePayrollItem.openingQuantity"
                  label="前月繰越日数"
                  readonly
                  variant="outlined"
                />
                <v-text-field
                  :model-value="activePayrollItem.accruedQuantity"
                  label="当月対象日数"
                  readonly
                  variant="outlined"
                />
                <v-text-field
                  :model-value="activePayrollItem.consumedQuantity"
                  label="支払い日数"
                  readonly
                  variant="outlined"
                />
                <v-text-field
                  :model-value="activePayrollItem.remainingQuantity"
                  label="残日数"
                  readonly
                  variant="outlined"
                />
              </div>
              <PayrollItemTransactionPanel
                v-if="
                  formModel.id > 0 &&
                  (activePayrollItem.inputSource === 'TRANSACTION' ||
                    activePayrollItem.parameters.collectionMode === 'MONTHLY')
                "
                :employee-id="formModel.id"
                :target-code="activePayrollItem.targetCode"
                :target-name="activePayrollItem.displayName"
                :quantity-unit="
                  activePayrollItem.balanceTracked ? activePayrollItem.balanceUnit : null
                "
              />
              <v-alert
                v-else-if="
                  formModel.id === 0 &&
                  (activePayrollItem.inputSource === 'TRANSACTION' ||
                    activePayrollItem.parameters.collectionMode === 'MONTHLY')
                "
                type="info"
                variant="tonal"
              >
                従業員を保存した後に明細を登録できます。
              </v-alert>
            </template>
          </v-card>
        </div>

        <FormGridTab
          v-else-if="active === 'payroll'"
          v-model="formModel.payrollProfile"
          :schema="payrollSchema"
          :fields="payrollFields"
        />

        <FormGridTab
          v-else-if="active === 'contract'"
          v-model="formModel.contract"
          :schema="contractSchema"
          :fields="contractFields"
        >
          <v-textarea
            v-model="formModel.contract.note"
            label="契約メモ"
            variant="outlined"
            rows="5"
            auto-grow
            hide-details
          />
        </FormGridTab>
      </template>
    </TabLayout>
  </AppDialog>

  <EmployeeResignDialog
    v-model="resignDialogVisible"
    :employee="formModel"
    :checklist="resignationChecklistQuery.checklist.value"
    :message="resignationChecklistQuery.message.value"
    @submit="handleResign"
  />
</template>

<style scoped>
.payroll-item-settings {
  padding: 16px;
}
.balance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
@media (max-width: 760px) {
  .balance-grid {
    grid-template-columns: 1fr;
  }
}
</style>
