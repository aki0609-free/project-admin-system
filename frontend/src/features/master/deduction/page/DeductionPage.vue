<script setup lang="ts">
import { computed, ref } from 'vue'
import CardLayout from '@/shared/components/layout/card_layout/CardLayout.vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import DeductionFormDialog from '@/features/master/deduction/components/DeductionFormDialog.vue'
import { useDeductionColumns } from '@/features/master/deduction/composables/useDeductionColumns'
import type {
  DeductionListItem,
  DeductionMaster,
} from '@/features/master/deduction/types/deductionTypes'

import { useDeductionsQuery } from '@/features/master/deduction/api/useDeductionsQuery'
import { useDeductionDetailQuery } from '@/features/master/deduction/api/useDeductionDetailQuery'
import { useCreateDeductionMutation } from '@/features/master/deduction/api/useCreateDeductionMutation'
import { useUpdateDeductionMutation } from '@/features/master/deduction/api/useUpdateDeductionMutation'
import { useDeleteDeductionMutation } from '@/features/master/deduction/api/useDeleteDeductionMutation'

import {
  toDeductionListItem,
  toDeductionMaster,
  toDeductionSaveRequest,
} from '@/features/master/deduction/mapper/deductionMapper'

const { columns } = useDeductionColumns()

const dialog = ref(false)
const isCreateMode = ref(false)
const selectedDeductionId = ref<number | null>(null)
const editingDeduction = ref<DeductionMaster | null>(null)

const deductionsQuery = useDeductionsQuery()
const deductionDetailQuery = useDeductionDetailQuery(selectedDeductionId)

const createMutation = useCreateDeductionMutation()
const updateMutation = useUpdateDeductionMutation()
const deleteMutation = useDeleteDeductionMutation()

const items = computed<DeductionListItem[]>(() =>
  deductionsQuery.deductions.value.map(toDeductionListItem),
)

const detail = computed(() => deductionDetailQuery.data.value ?? null)

const filterRules = computed(() =>
  createSimpleTableFilterRules<DeductionListItem>(columns.value),
)

const toolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '新規登録',
    color: 'primary',
    onClick: handleCreate,
  },
])

function createEmptyDeduction(): DeductionMaster {
  return {
    id: -Date.now(),
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
  }
}

function handleCreate() {
  isCreateMode.value = true
  selectedDeductionId.value = null
  editingDeduction.value = createEmptyDeduction()
  dialog.value = true
}

function handleEdit(row: DeductionListItem) {
  isCreateMode.value = false
  selectedDeductionId.value = row.id
  editingDeduction.value = toDeductionMaster(row)
  dialog.value = true
}

async function handleSave(payload: DeductionMaster) {
  const body = toDeductionSaveRequest(payload)

  if (isCreateMode.value) {
    await createMutation.mutateAsync(body)
  } else {
    await updateMutation.mutateAsync({
      id: payload.id,
      body,
    })
  }

  dialog.value = false
  selectedDeductionId.value = null
  editingDeduction.value = null
}

async function handleDelete(id: number) {
  await deleteMutation.mutateAsync(id)

  dialog.value = false
  selectedDeductionId.value = null
  editingDeduction.value = null
}
</script>

<template>
  <CardLayout title="控除マスター" subtitle="控除項目一覧">
    <div class="d-flex flex-column ga-4">
      <GenericToolbar :items="toolbarItems" />

      <v-alert
        v-if="deductionsQuery.isError.value"
        type="error"
        variant="tonal"
      >
        控除マスターの取得に失敗しました。
      </v-alert>

      <SimpleTable
        table-key="deduction-master"
        item-key="id"
        :items="items"
        :columns="columns"
        :filter-rules="filterRules"
        :enable-row-click="true"
        @row-click="handleEdit"
      />

      <DeductionFormDialog
        v-model="dialog"
        :deduction="editingDeduction"
        :detail-response="detail"
        :is-create-mode="isCreateMode"
        @save="handleSave"
        @delete="handleDelete"
      />
    </div>
  </CardLayout>
</template>