<script setup lang="ts">
import { computed, ref } from 'vue'
import CardLayout from '@/shared/components/layout/card_layout/CardLayout.vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import AllowanceFormDialog from '@/features/master/allowance/components/AllowanceFormDialog.vue'
import { useAllowanceColumns } from '@/features/master/allowance/composables/useAllowanceColumns'
import type {
  AllowanceListItem,
  AllowanceMaster,
} from '@/features/master/allowance/types/allowanceTypes'

import { useAllowancesQuery } from '@/features/master/allowance/api/useAllowancesQuery'
import { useCreateAllowanceMutation } from '@/features/master/allowance/api/useCreateAllowanceMutation'
import { useUpdateAllowanceMutation } from '@/features/master/allowance/api/useUpdateAllowanceMutation'
import { useDeleteAllowanceMutation } from '@/features/master/allowance/api/useDeleteAllowanceMutation'

import {
  toAllowanceListItem,
  toAllowanceMaster,
  toAllowanceSaveRequest,
} from '@/features/master/allowance/mapper/allowanceMapper'

const { columns } = useAllowanceColumns()

const dialog = ref(false)
const isCreateMode = ref(false)
const editingAllowance = ref<AllowanceMaster | null>(null)

const allowancesQuery = useAllowancesQuery()
const createMutation = useCreateAllowanceMutation()
const updateMutation = useUpdateAllowanceMutation()
const deleteMutation = useDeleteAllowanceMutation()

const items = computed<AllowanceListItem[]>(() =>
  allowancesQuery.allowances.value.map(toAllowanceListItem),
)

const filterRules = computed(() =>
  createSimpleTableFilterRules<AllowanceListItem>(columns.value),
)

const toolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '新規登録',
    color: 'primary',
    onClick: handleCreate,
  },
])

function createEmptyAllowance(): AllowanceMaster {
  return {
    id: -Date.now(),
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
  }
}

function handleCreate() {
  isCreateMode.value = true
  editingAllowance.value = createEmptyAllowance()
  dialog.value = true
}

function handleEdit(row: AllowanceListItem) {
  isCreateMode.value = false
  editingAllowance.value = toAllowanceMaster(row)
  dialog.value = true
}

async function handleSave(payload: AllowanceMaster) {
  const body = toAllowanceSaveRequest(payload)

  if (isCreateMode.value) {
    await createMutation.mutateAsync(body)
  } else {
    await updateMutation.mutateAsync({
      id: payload.id,
      body,
    })
  }

  dialog.value = false
  editingAllowance.value = null
}

async function handleDelete(id: number) {
  await deleteMutation.mutateAsync(id)

  dialog.value = false
  editingAllowance.value = null
}
</script>

<template>
  <CardLayout title="手当マスター" subtitle="手当項目一覧">
    <div class="d-flex flex-column ga-4">
      <GenericToolbar :items="toolbarItems" />

      <v-alert
        v-if="allowancesQuery.isError.value"
        type="error"
        variant="tonal"
      >
        手当マスターの取得に失敗しました。
      </v-alert>

      <SimpleTable
        table-key="allowance-master"
        item-key="id"
        :items="items"
        :columns="columns"
        :filter-rules="filterRules"
        :enable-row-click="true"
        @row-click="handleEdit"
      />

      <AllowanceFormDialog
        v-model="dialog"
        :allowance="editingAllowance"
        :is-create-mode="isCreateMode"
        @save="handleSave"
        @delete="handleDelete"
      />
    </div>
  </CardLayout>
</template>