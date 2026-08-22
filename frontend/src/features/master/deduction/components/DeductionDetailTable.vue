<script setup lang="ts">
import { computed, ref } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type {
  DeductionDetailTableRow,
  DeductionDetailViewType,
} from '@/features/master/deduction/types/deductionTypes'
import type { DeductionDetailResponse } from '@/features/master/deduction/types/deductionApiTypes'
import { useDeductionDetailConfig } from '@/features/master/deduction/composables/useDeductionDetailConfig'
import { toDeductionDetailRows } from '@/features/master/deduction/mapper/deductionMapper'
import ResidentTaxEditorDialog from '@/features/master/deduction/components/ResidentTaxEditorDialog.vue'
import { useAuth } from '@/shared/auth/composables/useAuth'
import { Role } from '@/shared/auth/types/types'

const props = defineProps<{
  deductionId: number
  detailViewType: DeductionDetailViewType
  detailResponse: DeductionDetailResponse | null
  targetDate: string
}>()

const emit = defineEmits<{
  (e: 'update:targetDate', value: string): void
}>()

const targetDateModel = computed({
  get: () => props.targetDate,
  set: (value: string) => emit('update:targetDate', value),
})

const detail = useDeductionDetailConfig(computed(() => props.detailViewType))
const { hasRole } = useAuth()
const residentTaxEditorOpen = ref(false)
const canEditResidentTax = computed(() =>
  props.detailViewType === 'RESIDENT_TAX' && hasRole(Role.SYS_ADMIN),
)

const rows = computed<DeductionDetailTableRow[]>(() =>
  toDeductionDetailRows(props.detailResponse?.details, props.detailViewType),
)
</script>

<template>
  <div class="d-flex flex-column ga-3">
    <v-text-field
      v-model="targetDateModel"
      type="date"
      label="参照基準日"
      variant="outlined"
      density="compact"
      hide-details
      max-width="280"
    />

    <div v-if="canEditResidentTax" class="d-flex justify-end">
      <v-btn color="primary" prepend-icon="mdi-table-edit" @click="residentTaxEditorOpen = true">
        年度別住民税を編集
      </v-btn>
    </div>
    <v-alert type="info" variant="tonal">
      <div class="font-weight-bold">{{ detail.title.value }}</div>
      <div>{{ detail.description.value }}</div>
      <div class="text-caption mt-1">
        計算根拠を確認するための読み取り専用表示です。
      </div>
    </v-alert>

    <v-alert v-if="rows.length === 0" type="warning" variant="tonal">
      選択した基準日に対応する詳細データが登録されていません。
    </v-alert>

    <SimpleTable
      v-else
      table-key="deduction-detail-reference"
      item-key="id"
      :items="rows"
      :columns="detail.columns.value"
      :filter-rules="detail.filterRules.value"
    />

    <ResidentTaxEditorDialog v-model="residentTaxEditorOpen" />
  </div>
</template>
