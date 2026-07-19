<script setup lang="ts">
import { computed, watch } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type {
  DeductionDetailTableRow,
  DeductionDetailViewType,
} from '@/features/master/deduction/types/deductionTypes'
import type { DeductionDetailResponse } from '@/features/master/deduction/types/deductionApiTypes'
import { useDeductionDetailConfig } from '@/features/master/deduction/composables/useDeductionDetailConfig'
import { toDeductionDetailRows } from '@/features/master/deduction/mapper/deductionMapper'
import { useEditableGenericDetailRows } from '@/features/master/deduction/composables/useEditableGenericDetailRows'
import { useCreateDeductionGenericSettingMutation } from '@/features/master/deduction/api/useCreateDeductionGenericSettingMutation'
import { useUpdateDeductionGenericSettingMutation } from '@/features/master/deduction/api/useUpdateDeductionGenericSettingMutation'
import { useDeleteDeductionGenericSettingMutation } from '@/features/master/deduction/api/useDeleteDeductionGenericSettingMutation'

const props = defineProps<{
  deductionId: number
  detailViewType: DeductionDetailViewType
  detailResponse: DeductionDetailResponse | null
}>()

const detail = useDeductionDetailConfig(computed(() => props.detailViewType))

const rows = computed<DeductionDetailTableRow[]>(() =>
  toDeductionDetailRows(props.detailResponse?.details, props.detailViewType),
)

const editableRows = useEditableGenericDetailRows(rows.value)

watch(
  () => rows.value,
  value => editableRows.reset(value),
  { immediate: true },
)

const createMutation = useCreateDeductionGenericSettingMutation()
const updateMutation = useUpdateDeductionGenericSettingMutation()
const deleteMutation = useDeleteDeductionGenericSettingMutation()

const isGeneric = computed(() => props.detailViewType === 'GENERIC')

const loading = computed(() =>
  createMutation.isPending.value ||
  updateMutation.isPending.value ||
  deleteMutation.isPending.value,
)

function toRequest(row: Record<string, unknown>) {
  return {
    deductionMasterId: props.deductionId,
    defaultAmount:
      row.defaultAmount == null || row.defaultAmount === ''
        ? null
        : Number(row.defaultAmount),
    amountType:
      row.amountType == null || row.amountType === ''
        ? null
        : String(row.amountType),
    allowManualInput: Boolean(row.allowManualInput),
    minAmount:
      row.minAmount == null || row.minAmount === ''
        ? null
        : Number(row.minAmount),
    maxAmount:
      row.maxAmount == null || row.maxAmount === ''
        ? null
        : Number(row.maxAmount),
    targetScope:
      row.targetScope == null || row.targetScope === ''
        ? null
        : String(row.targetScope),
    displayOrder:
      row.displayOrder == null || row.displayOrder === ''
        ? null
        : Number(row.displayOrder),
    enabled: row.enabled == null ? true : Boolean(row.enabled),
    note:
      row.note == null || row.note === ''
        ? null
        : String(row.note),
  }
}

function handleAdd() {
  editableRows.addRow({
    id: -Date.now(),
    detailType: 'GENERIC',
    label: '新規一般控除設定',
    defaultAmount: null,
    amountType: 'VARIABLE',
    allowManualInput: true,
    minAmount: null,
    maxAmount: null,
    targetScope: 'EMPLOYEE',
    displayOrder: 999,
    enabled: true,
    note: '',
  })
}

function handleDeleteSelected() {
  editableRows.markSelectedDeleted()
}

async function handleSave() {
  for (const row of editableRows.changedRows.value) {
    if (row._isDeleted) {
      if (!row._isNew) {
        await deleteMutation.mutateAsync(Number(row.id))
      }
      continue
    }

    if (row._isNew) {
      await createMutation.mutateAsync(toRequest(row))
      continue
    }

    if (row._isUpdated) {
      await updateMutation.mutateAsync({
        id: Number(row.id),
        body: toRequest(row),
      })
    }
  }

  editableRows.reset(rows.value)
}
</script>

<template>
  <div class="d-flex flex-column ga-3">
    <v-alert type="info" variant="tonal">
      <div class="font-weight-bold">{{ detail.title.value }}</div>
      <div>{{ detail.description.value }}</div>
      <div class="text-caption mt-1">控除ID: {{ deductionId }}</div>
    </v-alert>

    <div v-if="isGeneric" class="d-flex justify-space-between">
      <div class="d-flex ga-2">
        <v-btn color="primary" variant="tonal" @click="handleAdd">
          行追加
        </v-btn>

        <v-btn
          color="error"
          variant="tonal"
          :disabled="!editableRows.hasDeleteSelected.value"
          @click="handleDeleteSelected"
        >
          選択行を削除
        </v-btn>
      </div>

      <v-btn
        color="primary"
        :loading="loading"
        :disabled="editableRows.changedRows.value.length === 0"
        @click="handleSave"
      >
        詳細設定を保存
      </v-btn>
    </div>

    <SimpleTable
      tableKey="deduction-detail-dynamic"
      itemKey="id"
      :items="isGeneric ? editableRows.visibleRows.value : rows"
      :columns="detail.columns.value"
      :filterRules="detail.filterRules.value"
      @update:items="editableRows.updateCell"
    />
  </div>
</template>