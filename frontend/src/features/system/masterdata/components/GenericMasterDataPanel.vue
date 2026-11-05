<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import AppToolbar from '@/shared/ui/toolbar/AppToolbar.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import {
  createGenericMasterRecord,
  deleteGenericMasterRecord,
  getGenericMasterRecords,
  updateGenericMasterRecord,
} from '../api/genericMasterApi'
import type {
  GenericMasterDefinition,
  GenericMasterFieldDefinition,
  GenericMasterRecord,
  GenericMasterSelectOption,
} from '../types/genericMasterTypes'

const props = defineProps<{
  definition: GenericMasterDefinition
}>()

type RecordRow = GenericMasterRecord & Record<string, unknown> & {
  activeLabel: string
}

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const records = ref<GenericMasterRecord[]>([])
const dialog = ref(false)
const editingRecord = ref<GenericMasterRecord | null>(null)
const formValues = reactive<Record<string, unknown>>({})
const activeFlag = ref(true)

const parseOptions = (field: GenericMasterFieldDefinition): GenericMasterSelectOption[] => {
  try {
    const options = JSON.parse(field.optionsJson || '[]') as Array<
      string | number | boolean | GenericMasterSelectOption
    >
    return options.map(option => typeof option === 'object'
      ? option
      : { title: String(option), value: option })
  } catch {
    return []
  }
}

const formatValue = (field: GenericMasterFieldDefinition, value: unknown) => {
  if (value == null || value === '') return ''
  if (field.fieldType === 'BOOLEAN') return value === true || value === 'true' ? '有効' : '無効'
  if (field.fieldType === 'SELECT') {
    return parseOptions(field).find(option => String(option.value) === String(value))?.title
      ?? String(value)
  }
  if (field.fieldType === 'NUMBER' && field.unit) return `${value}${field.unit}`
  return String(value)
}

const rows = computed<RecordRow[]>(() => records.value.map(record => {
  const row: RecordRow = {
    ...record,
    activeLabel: record.activeFlag ? '有効' : '無効',
  }
  for (const field of props.definition.fields) {
    row[field.fieldKey] = formatValue(field, record.values[field.fieldKey])
  }
  return row
}))

const visibleFields = computed(() => props.definition.fields.filter(field => field.listVisibleFlag))
const columns = computed<SimpleTableColumnDef<RecordRow>[]>(() => [
  ...visibleFields.value.map(field => ({
    title: field.fieldLabel,
    key: field.fieldKey,
    width: field.fieldType === 'TEXT' ? '280px' : '180px',
    filter: field.searchableFlag
      ? { type: field.fieldType === 'NUMBER' ? 'number' as const : 'text' as const }
      : undefined,
  })),
  {
    title: '状態',
    key: 'activeLabel',
    width: '110px',
    filter: { type: 'select' },
    enumOptions: [
      { title: '有効', value: '有効' },
      { title: '無効', value: '無効' },
    ],
  },
])
const filterRules = computed(() => createSimpleTableFilterRules(columns.value))
const toolbarItems = computed<ToolbarItem[]>(() => [{
  type: 'button',
  label: `${props.definition.tabName}を追加`,
  intent: 'primary',
  onClick: () => openCreate(),
}])

const run = async (action: () => Promise<void>, successText?: string) => {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await action()
    if (successText) successMessage.value = successText
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '処理に失敗しました。'
  } finally {
    loading.value = false
  }
}

const load = () => run(async () => {
  records.value = await getGenericMasterRecords(props.definition.id)
})

const resetForm = () => {
  for (const key of Object.keys(formValues)) formValues[key] = undefined
  for (const field of props.definition.fields) {
    formValues[field.fieldKey] = field.fieldType === 'BOOLEAN' ? false : null
  }
  activeFlag.value = true
}

const openCreate = () => {
  editingRecord.value = null
  resetForm()
  dialog.value = true
}

const openEdit = (row: RecordRow) => {
  const record = records.value.find(item => item.id === row.id)
  if (!record) return
  editingRecord.value = record
  resetForm()
  Object.assign(formValues, record.values)
  activeFlag.value = record.activeFlag
  dialog.value = true
}

const validate = () => {
  for (const field of props.definition.fields) {
    const value = formValues[field.fieldKey]
    if (field.requiredFlag && (value == null || value === '')) {
      errorMessage.value = `${field.fieldLabel}は必須です。`
      return false
    }
    if (field.validationPattern && value != null && value !== '') {
      try {
        if (!new RegExp(field.validationPattern).test(String(value))) {
          errorMessage.value = `${field.fieldLabel}の入力規則に一致しません。`
          return false
        }
      } catch {
        errorMessage.value = `${field.fieldLabel}の入力規則が不正です。`
        return false
      }
    }
  }
  return true
}

const save = () => {
  if (!validate()) return
  return run(async () => {
    const request = {
      values: { ...formValues },
      activeFlag: activeFlag.value,
      lockVersion: editingRecord.value?.lockVersion ?? null,
    }
    if (editingRecord.value) {
      await updateGenericMasterRecord(
        props.definition.id,
        editingRecord.value.id,
        request,
      )
    } else {
      await createGenericMasterRecord(props.definition.id, request)
    }
    records.value = await getGenericMasterRecords(props.definition.id)
    dialog.value = false
  }, `${props.definition.tabName}を保存しました。`)
}

const remove = () => {
  const record = editingRecord.value
  if (!record || !window.confirm('このデータを削除しますか？')) return
  return run(async () => {
    await deleteGenericMasterRecord(props.definition.id, record.id)
    records.value = await getGenericMasterRecords(props.definition.id)
    dialog.value = false
  }, `${props.definition.tabName}を削除しました。`)
}

watch(() => props.definition.id, () => void load())
onMounted(() => void load())
</script>

<template>
  <section class="generic-data-panel">
    <div class="panel-heading">
      <div>
        <h2>{{ definition.masterName }}</h2>
        <p v-if="definition.description">{{ definition.description }}</p>
      </div>
    </div>
    <v-alert v-if="errorMessage" type="error" variant="tonal" closable @click:close="errorMessage = ''">
      {{ errorMessage }}
    </v-alert>
    <v-alert v-if="successMessage" type="success" variant="tonal" closable @click:close="successMessage = ''">
      {{ successMessage }}
    </v-alert>
    <AppToolbar :right-items="toolbarItems" surface="page" />
    <v-progress-linear v-if="loading" indeterminate color="primary" />
    <SimpleTable
      :table-key="`generic-master-data-${definition.masterCode}`"
      item-key="id"
      :items="rows"
      :columns="columns"
      :filter-rules="filterRules"
      :enable-row-click="true"
      @row-click="openEdit"
    />

    <v-dialog v-model="dialog" max-width="900" persistent>
      <v-card v-if="dialog">
        <v-card-title>{{ editingRecord ? `${definition.tabName}を編集` : `${definition.tabName}を追加` }}</v-card-title>
        <v-card-text>
          <div class="form-grid">
            <template v-for="field in definition.fields" :key="field.fieldKey">
              <v-textarea
                v-if="field.fieldType === 'TEXT'"
                v-model="formValues[field.fieldKey]"
                :label="field.fieldLabel"
                :placeholder="field.placeholder ?? undefined"
                :required="field.requiredFlag"
                rows="3"
                variant="outlined"
                class="full-width"
              />
              <v-select
                v-else-if="field.fieldType === 'SELECT'"
                v-model="formValues[field.fieldKey]"
                :label="field.fieldLabel"
                :items="parseOptions(field)"
                :required="field.requiredFlag"
                variant="outlined"
              />
              <v-switch
                v-else-if="field.fieldType === 'BOOLEAN'"
                v-model="formValues[field.fieldKey]"
                :label="field.fieldLabel"
                color="primary"
              />
              <v-text-field
                v-else
                v-model="formValues[field.fieldKey]"
                :label="field.fieldLabel"
                :type="field.fieldType === 'NUMBER' ? 'number' : field.fieldType === 'DATE' ? 'date' : 'text'"
                :suffix="field.unit ?? undefined"
                :placeholder="field.placeholder ?? undefined"
                :required="field.requiredFlag"
                variant="outlined"
              />
            </template>
          </div>
          <v-switch v-model="activeFlag" label="有効" color="primary" />
        </v-card-text>
        <v-card-actions>
          <v-btn v-if="editingRecord" color="error" variant="text" @click="remove">削除</v-btn>
          <v-spacer />
          <v-btn variant="text" @click="dialog = false">キャンセル</v-btn>
          <v-btn color="primary" :loading="loading" @click="save">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </section>
</template>

<style scoped>
.generic-data-panel {
  display: grid;
  gap: 16px;
  padding: 24px;
  background: #fff;
}

.panel-heading h2,
.panel-heading p {
  margin: 0;
}

.panel-heading p {
  margin-top: 6px;
  color: rgba(var(--v-theme-on-surface), 0.72);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 16px;
}

.full-width {
  grid-column: 1 / -1;
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
