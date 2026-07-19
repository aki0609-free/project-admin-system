<script setup lang="ts">
import { computed } from 'vue'
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import MultiLevelHeaderTable from '@/shared/components/table/multi_level_header_table/MultiLevelHeaderTable.vue'
import type { ApplicationMediaPivotRow, FilterState } from '@/features/application/types/applicationMediaTypes'
import { useApplicationMediaTable } from '@/features/application/composables/application_media/useApplicationMediaTable'
import { useApplicationMediaTableDialog } from '@/features/application/composables/application_media/useApplicationMediaDialog'
import { useApplicationMediaTableToolbar } from '@/features/application/composables/application_media/useApplicationMediaTableToolbar'
import { yearOptions, monthOptions } from '@/shared/utils/DateUtils'

const TABLE_KEY = 'application-media-table'

const props = defineProps<{
  yearMonths: string[]
  allYearMonthOptions: { label: string; value: string }[]
  mediaNames: string[]
  filter: FilterState
  pivotTableData: ApplicationMediaPivotRow[]
  isDirty: boolean
  saving?: boolean
  onSave: () => void | Promise<void>
}>()

const emit = defineEmits<{
  (e: 'update-cell', payload: {
    id: number
    field: keyof ApplicationMediaPivotRow
    value: unknown
  }): void
  (e: 'add-media', name: string): void
  (e: 'add-year-month', value: string): void
  (e: 'delete-media', value: string): void
  (e: 'delete-year-month', value: string): void
}>()

const dialogState = useApplicationMediaTableDialog()

const table = useApplicationMediaTable(
  computed(() => props.yearMonths),
)

const toolbar = useApplicationMediaTableToolbar({
  allYearMonthOptions: computed(() => props.allYearMonthOptions),
  filter: props.filter,
  openMediaDialog: dialogState.openMediaDialog,
  openYearMonthDialog: dialogState.openYearMonthDialog,
  openDeleteMediaDialog: dialogState.openDeleteMediaDialog,
  openDeleteYearMonthDialog: dialogState.openDeleteYearMonthDialog,
  isDirty: computed(() => props.isDirty),
  saving: computed(() => props.saving ?? false),
  onSave: props.onSave,
})

const canAddMedia = computed(() => dialogState.newMediaName.value.trim().length > 0)
const canAddYearMonth = computed(() => !!dialogState.newYear.value && !!dialogState.newMonth.value)
const canDeleteMedia = computed(() => !!dialogState.deleteMediaName.value)
const canDeleteYearMonth = computed(() => !!dialogState.deleteYearMonthValue.value)

function addMedia() {
  const name = dialogState.newMediaName.value.trim()
  if (!name) return

  emit('add-media', name)
  dialogState.newMediaName.value = ''
  dialogState.closeMediaDialog()
}

function addYearMonth() {
  if (!dialogState.newYear.value || !dialogState.newMonth.value) return

  const year = String(dialogState.newYear.value)
  const month = String(dialogState.newMonth.value).padStart(2, '0')
  const value = `${year}-${month}`

  emit('add-year-month', value)

  dialogState.newYear.value = ''
  dialogState.newMonth.value = ''
  dialogState.closeYearMonthDialog()
}

function deleteMedia() {
  if (!dialogState.deleteMediaName.value) return
  emit('delete-media', dialogState.deleteMediaName.value)
  dialogState.deleteMediaName.value = ''
  dialogState.closeDeleteMediaDialog()
}

function deleteYearMonth() {
  if (!dialogState.deleteYearMonthValue.value) return
  emit('delete-year-month', dialogState.deleteYearMonthValue.value)
  dialogState.deleteYearMonthValue.value = ''
  dialogState.closeDeleteYearMonthDialog()
}
</script>

<template>
  <div class="d-flex flex-column ga-4">
    <GenericToolbar :items="toolbar.toolbarItems.value" />

    <MultiLevelHeaderTable
      :table-key="TABLE_KEY"
      :data="pivotTableData"
      :def="table.pivotTableDef.value"
      @update-cell="emit('update-cell', $event)"
    />

    <v-dialog v-model="dialogState.createMediaDialog.value" max-width="460">
      <v-card>
        <v-card-title>媒体追加</v-card-title>
        <v-card-text>
          <v-text-field
            v-model="dialogState.newMediaName.value"
            label="応募媒体名"
            placeholder="例：Indeed"
            density="comfortable"
            hide-details
            clearable
            @keydown.enter="addMedia"
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialogState.closeMediaDialog()">キャンセル</v-btn>
          <v-btn color="primary" :disabled="!canAddMedia" @click="addMedia">追加</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="dialogState.createYearMonthDialog.value" max-width="460">
      <v-card>
        <v-card-title>年月追加</v-card-title>
        <v-card-text>
          <div class="d-flex ga-2">
            <v-select
              v-model="dialogState.newYear.value"
              :items="yearOptions()"
              item-title="label"
              item-value="value"
              label="年"
              density="comfortable"
              hide-details
            />
            <v-select
              v-model="dialogState.newMonth.value"
              :items="monthOptions"
              item-title="label"
              item-value="value"
              label="月"
              density="comfortable"
              hide-details
            />
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialogState.closeYearMonthDialog()">キャンセル</v-btn>
          <v-btn color="primary" :disabled="!canAddYearMonth" @click="addYearMonth">追加</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="dialogState.deleteMediaDialog.value" max-width="460">
      <v-card>
        <v-card-title>メディア削除</v-card-title>
        <v-card-text>
          <v-select
            v-model="dialogState.deleteMediaName.value"
            :items="mediaNames"
            label="削除するメディア"
            density="comfortable"
            hide-details
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialogState.closeDeleteMediaDialog()">キャンセル</v-btn>
          <v-btn color="error" :disabled="!canDeleteMedia" @click="deleteMedia">削除</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="dialogState.deleteYearMonthDialog.value" max-width="460">
      <v-card>
        <v-card-title>年月削除</v-card-title>
        <v-card-text>
          <v-select
            v-model="dialogState.deleteYearMonthValue.value"
            :items="allYearMonthOptions"
            item-title="label"
            item-value="value"
            label="削除する年月"
            density="comfortable"
            hide-details
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialogState.closeDeleteYearMonthDialog()">キャンセル</v-btn>
          <v-btn color="error" :disabled="!canDeleteYearMonth" @click="deleteYearMonth">削除</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>