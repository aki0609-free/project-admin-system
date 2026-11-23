<script setup lang="ts">
import { ref, toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import { type DailyPreparationAssignmentTableRow, useDailyPreparationAssignmentTableConfig } from '../composables/useDailyPreparationAssignmentTableConfig';


const props = defineProps<{
  items: DailyPreparationAssignmentTableRow[]
}>()

const emit = defineEmits<{
  (
    e: 'update:items',
    payload: {
      id: number
      field: keyof DailyPreparationAssignmentTableRow
      value: unknown
    }
  ): void
  (e: 'reset-row', id: number): void
}>()

const resetTarget = ref<DailyPreparationAssignmentTableRow | null>(null)

const canReset = (item: DailyPreparationAssignmentTableRow) =>
  item.customerId != null ||
  item.customerSiteId != null ||
  item.workDescription.trim() !== '' ||
  item.vehicleArrangementType !== 'NONE' ||
  item.passengerCount > 0

const confirmReset = () => {
  if (!resetTarget.value) return
  emit('reset-row', resetTarget.value.id)
  resetTarget.value = null
}

const {
  rows,
  columns,
  filterRules,
} = useDailyPreparationAssignmentTableConfig(
  toRef(props, 'items'),
)
</script>

<template>
  <SimpleTable
    table-key="daily-preparation-assignment-list"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    @update:items="emit('update:items', $event)"
  >
    <template #[`item.resetAction`]="{ item }">
      <v-btn
        size="small"
        variant="outlined"
        color="error"
        :disabled="!canReset(item)"
        @click.stop="resetTarget = item"
      >
        初期化
      </v-btn>
    </template>
  </SimpleTable>

  <v-dialog :model-value="resetTarget != null" max-width="480" persistent>
    <v-card>
      <v-card-title>従業員配置を初期化</v-card-title>
      <v-card-text>
        {{ resetTarget?.employeeName }} の顧客・現場・車両手配・同乗者数・作業内容を初期化します。
        画面上で初期化後、「従業員配置保存」を押すと確定します。
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="resetTarget = null">キャンセル</v-btn>
        <v-btn color="error" variant="flat" @click="confirmReset">初期化</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
