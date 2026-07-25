<script setup lang="ts">
import { computed } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type {
  DeductionDetailTableRow,
  DeductionDetailViewType,
} from '@/features/master/deduction/types/deductionTypes'
import type { DeductionDetailResponse } from '@/features/master/deduction/types/deductionApiTypes'
import { useDeductionDetailConfig } from '@/features/master/deduction/composables/useDeductionDetailConfig'
import { toDeductionDetailRows } from '@/features/master/deduction/mapper/deductionMapper'

const props = defineProps<{
  deductionId: number
  detailViewType: DeductionDetailViewType
  detailResponse: DeductionDetailResponse | null
}>()

const detail = useDeductionDetailConfig(computed(() => props.detailViewType))

const rows = computed<DeductionDetailTableRow[]>(() =>
  toDeductionDetailRows(props.detailResponse?.details, props.detailViewType),
)
</script>

<template>
  <div class="d-flex flex-column ga-3">
    <v-alert type="info" variant="tonal">
      <div class="font-weight-bold">{{ detail.title.value }}</div>
      <div>{{ detail.description.value }}</div>
      <div class="text-caption mt-1">
        計算根拠を確認するための読み取り専用表示です。
      </div>
    </v-alert>

    <v-alert v-if="rows.length === 0" type="warning" variant="tonal">
      現在年度に対応する詳細データが登録されていません。
    </v-alert>

    <SimpleTable
      v-else
      table-key="deduction-detail-reference"
      item-key="id"
      :items="rows"
      :columns="detail.columns.value"
      :filter-rules="detail.filterRules.value"
    />
  </div>
</template>
