<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { EmployeeForm } from '../types/employeeFormTypes'
import PayrollItemTransactionPanel from './PayrollItemTransactionPanel.vue'
import EmployeePayrollItemParameterField from './EmployeePayrollItemParameterField.vue'

type Setting = EmployeeForm['payrollItemSettings'][number]

defineProps<{
  employeeId: number
}>()

const settings = defineModel<EmployeeForm['payrollItemSettings']>({ required: true })
const activeCode = ref('')
const settingKey = (item: Setting) => `${item.targetType}:${item.targetCode}`

watch(
  settings,
  (items) => {
    if (items.length && !items.some((item) => settingKey(item) === activeCode.value)) {
      activeCode.value = items[0] ? settingKey(items[0]) : ''
    }
  },
  { immediate: true, deep: true },
)

const activeItem = computed(() =>
  settings.value.find((item) => settingKey(item) === activeCode.value),
)

const visibleDefinitions = computed(
  () =>
    activeItem.value?.parameterDefinitions.filter(
      (definition) => !definition.ruleValueResolverKey,
    ) ?? [],
)

const effectiveInputSource = (item: Setting) => {
  const override = item.parameterDefinitions.find((definition) => definition.inputSourceOverride)
  const value = override ? item.parameters[override.key] : null
  return value === 'DAILY_REPORT' || value === 'TRANSACTION' ? value : item.inputSource
}

const unitLabel = (unit: string) =>
  ({
    DAYS: '日数',
    HOURS: '時間',
    COUNT: '回数',
    AMOUNT: '金額',
  })[unit] ?? '数量'

</script>

<template>
  <section class="payroll-item-settings">
    <v-alert v-if="settings.length === 0" type="info" variant="tonal">
      従業員別に設定する手当・控除はありません。
    </v-alert>

    <template v-else>
      <v-tabs v-model="activeCode" color="primary" show-arrows>
        <v-tab
          v-for="item in settings"
          :key="`${item.targetType}:${item.targetCode}`"
          :value="settingKey(item)"
        >
          {{ item.displayName }}
        </v-tab>
      </v-tabs>

      <v-card v-if="activeItem" variant="outlined" class="pa-5 mt-4">
        <div class="setting-header">
          <div>
            <div class="text-subtitle-1 font-weight-bold">
              {{ activeItem.displayName }}
            </div>
            <div class="text-caption text-medium-emphasis">
              {{ activeItem.targetType === 'ALLOWANCE' ? '手当' : '控除' }}・{{
                effectiveInputSource(activeItem) === 'DAILY_REPORT' ? '日報入力' : '明細入力'
              }}
            </div>
          </div>
          <v-switch
            v-model="activeItem.enabled"
            label="この項目を適用する"
            color="primary"
            hide-details
          />
        </div>

        <template v-if="activeItem.enabled">
          <div class="parameter-grid mt-5">
            <EmployeePayrollItemParameterField
              v-for="definition in visibleDefinitions"
              :key="definition.key"
              v-model="activeItem.parameters[definition.key]"
              :definition="definition"
            />
          </div>

          <div v-if="activeItem.balanceTracked" class="balance-section mt-4">
            <div class="text-subtitle-2 mb-3">残{{ unitLabel(activeItem.balanceUnit) }}</div>
            <div class="balance-grid">
              <v-text-field
                :model-value="activeItem.effectiveFrom"
                label="適用開始日"
                readonly
                density="compact"
                variant="outlined"
              />
              <v-text-field
                :model-value="activeItem.openingQuantity"
                :label="`前月繰越${unitLabel(activeItem.balanceUnit)}`"
                readonly
                density="compact"
                variant="outlined"
              />
              <v-text-field
                :model-value="activeItem.accruedQuantity"
                :label="`当月増加${unitLabel(activeItem.balanceUnit)}`"
                readonly
                density="compact"
                variant="outlined"
              />
              <v-text-field
                :model-value="activeItem.consumedQuantity"
                :label="`当月消化${unitLabel(activeItem.balanceUnit)}`"
                readonly
                density="compact"
                variant="outlined"
              />
              <v-text-field
                :model-value="activeItem.remainingQuantity"
                :label="`現在残${unitLabel(activeItem.balanceUnit)}`"
                readonly
                density="compact"
                variant="outlined"
              />
            </div>
          </div>

          <PayrollItemTransactionPanel
            v-if="employeeId > 0 && effectiveInputSource(activeItem) === 'TRANSACTION'"
            :employee-id="employeeId"
            :target-type="activeItem.targetType"
            :target-code="activeItem.targetCode"
            :target-name="activeItem.displayName"
            :quantity-unit="activeItem.balanceTracked ? activeItem.balanceUnit : null"
          />
          <v-alert
            v-else-if="employeeId === 0 && effectiveInputSource(activeItem) === 'TRANSACTION'"
            type="info"
            variant="tonal"
            class="mt-4"
          >
            従業員を保存した後に明細を登録できます。
          </v-alert>
        </template>
      </v-card>
    </template>
  </section>
</template>

<style scoped>
.payroll-item-settings {
  padding: 16px;
}
.setting-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}
.parameter-grid,
.balance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.balance-section {
  border-top: 1px solid rgba(0, 0, 0, 0.12);
  padding-top: 16px;
}
@media (max-width: 760px) {
  .setting-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .parameter-grid,
  .balance-grid {
    grid-template-columns: 1fr;
  }
}
</style>
