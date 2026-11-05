<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import AppToolbar from '@/shared/ui/toolbar/AppToolbar.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import {
  createGenericMasterDefinition,
  deleteGenericMasterDefinition,
  getGenericMasterDefinitions,
  publishGenericMasterDefinition,
  updateGenericMasterDefinition,
} from '../api/genericMasterApi'
import type {
  GenericMasterDefinition,
  GenericMasterDefinitionSaveRequest,
  GenericMasterFieldDefinition,
  GenericMasterFieldType,
} from '../types/genericMasterTypes'

type DefinitionRow = GenericMasterDefinition & {
  statusLabel: string
  activeLabel: string
  fieldCount: number
}

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const definitions = ref<GenericMasterDefinition[]>([])
const dialog = ref(false)
const editingId = ref<number | null>(null)
const editingStatus = ref<'DRAFT' | 'PUBLISHED'>('DRAFT')

const emptyField = (order: number): GenericMasterFieldDefinition => ({
  id: null,
  fieldKey: '',
  fieldLabel: '',
  fieldType: 'STRING',
  requiredFlag: false,
  listVisibleFlag: true,
  searchableFlag: true,
  displayOrder: order,
  unit: null,
  placeholder: null,
  validationPattern: null,
  optionsJson: null,
})

const emptyForm = (): GenericMasterDefinitionSaveRequest => ({
  masterCode: '',
  masterName: '',
  tabName: '',
  placement: 'BUSINESS_SETTINGS',
  description: '',
  displayOrder: 100,
  activeFlag: true,
  fields: [emptyField(10)],
})

const form = reactive<GenericMasterDefinitionSaveRequest>(emptyForm())

const fieldTypes: Array<{ title: string, value: GenericMasterFieldType }> = [
  { title: '文字列', value: 'STRING' },
  { title: '複数行テキスト', value: 'TEXT' },
  { title: '数値', value: 'NUMBER' },
  { title: '日付', value: 'DATE' },
  { title: 'ON/OFF', value: 'BOOLEAN' },
  { title: '選択肢', value: 'SELECT' },
]

const rows = computed<DefinitionRow[]>(() => definitions.value.map(item => ({
  ...item,
  statusLabel: item.status === 'PUBLISHED' ? '公開' : '下書き',
  activeLabel: item.activeFlag ? '有効' : '無効',
  fieldCount: item.fields.length,
})))

const columns = computed<SimpleTableColumnDef<DefinitionRow>[]>(() => [
  { title: 'マスターコード', key: 'masterCode', width: '220px', filter: { type: 'text' } },
  { title: 'マスター名', key: 'masterName', width: '240px', filter: { type: 'text' } },
  { title: '業務管理タブ名', key: 'tabName', width: '200px', filter: { type: 'text' } },
  { title: '項目数', key: 'fieldCount', width: '100px', filter: { type: 'number' } },
  {
    title: '公開状態',
    key: 'statusLabel',
    width: '120px',
    filter: { type: 'select' },
    enumOptions: [
      { title: '下書き', value: '下書き' },
      { title: '公開', value: '公開' },
    ],
  },
  {
    title: '状態',
    key: 'activeLabel',
    width: '100px',
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
  label: '汎用マスターを追加',
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
  definitions.value = await getGenericMasterDefinitions()
})

const openCreate = () => {
  editingId.value = null
  editingStatus.value = 'DRAFT'
  Object.assign(form, emptyForm())
  dialog.value = true
}

const openEdit = (item: DefinitionRow) => {
  editingId.value = item.id
  editingStatus.value = item.status
  Object.assign(form, {
    masterCode: item.masterCode,
    masterName: item.masterName,
    tabName: item.tabName,
    placement: item.placement,
    description: item.description ?? '',
    displayOrder: item.displayOrder,
    activeFlag: item.activeFlag,
    fields: item.fields.map(field => ({ ...field })),
  })
  dialog.value = true
}

const addField = () => {
  const nextOrder = Math.max(0, ...form.fields.map(field => field.displayOrder)) + 10
  form.fields.push(emptyField(nextOrder))
}

const removeField = (index: number) => {
  if (form.fields.length <= 1) {
    errorMessage.value = '項目は1件以上必要です。'
    return
  }
  form.fields.splice(index, 1)
}

const validate = () => {
  const keyPattern = /^[A-Z][A-Z0-9_]*$/
  if (!keyPattern.test(form.masterCode)) {
    errorMessage.value = 'マスターコードは半角英大文字・数字・アンダースコアで入力してください。'
    return false
  }
  if (!form.masterName.trim() || !form.tabName.trim()) {
    errorMessage.value = 'マスター名と業務管理タブ名は必須です。'
    return false
  }
  const keys = new Set<string>()
  for (const field of form.fields) {
    if (!keyPattern.test(field.fieldKey) || !field.fieldLabel.trim()) {
      errorMessage.value = 'すべての項目に有効な項目キーと項目名を指定してください。'
      return false
    }
    if (keys.has(field.fieldKey)) {
      errorMessage.value = `項目キーが重複しています: ${field.fieldKey}`
      return false
    }
    keys.add(field.fieldKey)
    if (field.fieldType === 'SELECT') {
      try {
        const options = JSON.parse(field.optionsJson || '')
        if (!Array.isArray(options) || options.length === 0) throw new Error()
      } catch {
        errorMessage.value = `${field.fieldLabel}の選択肢はJSON配列で指定してください。`
        return false
      }
    }
  }
  return true
}

const save = () => {
  if (!validate()) return
  return run(async () => {
    const request: GenericMasterDefinitionSaveRequest = {
      masterCode: form.masterCode,
      masterName: form.masterName,
      tabName: form.tabName,
      placement: form.placement,
      description: form.description,
      displayOrder: Number(form.displayOrder),
      activeFlag: form.activeFlag,
      fields: form.fields.map(field => ({ ...field })),
    }
    if (editingId.value == null) {
      await createGenericMasterDefinition(request)
    } else {
      await updateGenericMasterDefinition(editingId.value, request)
    }
    definitions.value = await getGenericMasterDefinitions()
    dialog.value = false
  }, '汎用マスター定義を保存しました。')
}

const publish = () => {
  if (editingId.value == null || !window.confirm('この定義を業務管理へ公開しますか？')) return
  return run(async () => {
    await publishGenericMasterDefinition(editingId.value as number)
    definitions.value = await getGenericMasterDefinitions()
    dialog.value = false
  }, '汎用マスターを業務管理へ公開しました。')
}

const remove = () => {
  if (editingId.value == null || !window.confirm('この汎用マスター定義を削除しますか？')) return
  return run(async () => {
    await deleteGenericMasterDefinition(editingId.value as number)
    definitions.value = await getGenericMasterDefinitions()
    dialog.value = false
  }, '汎用マスター定義を削除しました。')
}

onMounted(() => void load())
</script>

<template>
  <div class="generic-master-panel">
    <v-alert v-if="errorMessage" type="error" variant="tonal" closable @click:close="errorMessage = ''">
      {{ errorMessage }}
    </v-alert>
    <v-alert v-if="successMessage" type="success" variant="tonal" closable @click:close="successMessage = ''">
      {{ successMessage }}
    </v-alert>

    <AppToolbar :right-items="toolbarItems" surface="page" />
    <p class="help-text">
      項目を定義して公開すると、業務管理に入力用タブが自動表示されます。
    </p>
    <v-progress-linear v-if="loading" indeterminate color="primary" />
    <SimpleTable
      table-key="generic-master-definition"
      item-key="id"
      :items="rows"
      :columns="columns"
      :filter-rules="filterRules"
      :enable-row-click="true"
      @row-click="openEdit"
    />

    <v-dialog v-model="dialog" max-width="1180" persistent>
      <v-card v-if="dialog">
        <v-card-title>{{ editingId == null ? '汎用マスターを追加' : '汎用マスターを編集' }}</v-card-title>
        <v-card-text>
          <v-alert type="info" variant="tonal" class="mb-5">
            公開済みでデータが登録された後は、項目キーと型を変更できません。
          </v-alert>
          <div class="definition-grid">
            <v-text-field v-model="form.masterCode" label="マスターコード" :readonly="editingId != null" variant="outlined" />
            <v-text-field v-model="form.masterName" label="マスター名" variant="outlined" />
            <v-text-field v-model="form.tabName" label="業務管理タブ名" variant="outlined" />
            <v-text-field v-model.number="form.displayOrder" label="表示順" type="number" variant="outlined" />
          </div>
          <v-textarea v-model="form.description" label="説明" rows="2" variant="outlined" />
          <v-switch v-model="form.activeFlag" label="有効" color="primary" />

          <div class="field-heading">
            <h3>入力項目</h3>
            <v-btn variant="tonal" color="primary" @click="addField">項目を追加</v-btn>
          </div>
          <v-card
            v-for="(field, index) in form.fields"
            :key="`${field.fieldKey}-${index}`"
            variant="outlined"
            class="field-card"
          >
            <div class="field-grid">
              <v-text-field v-model="field.fieldKey" label="項目キー" :readonly="editingStatus === 'PUBLISHED' && field.id != null" variant="outlined" />
              <v-text-field v-model="field.fieldLabel" label="項目名" variant="outlined" />
              <v-select v-model="field.fieldType" label="入力形式" :items="fieldTypes" :readonly="editingStatus === 'PUBLISHED' && field.id != null" variant="outlined" />
              <v-text-field v-model.number="field.displayOrder" label="表示順" type="number" variant="outlined" />
              <v-text-field v-model="field.unit" label="単位" variant="outlined" />
              <v-text-field v-model="field.placeholder" label="入力例" variant="outlined" />
              <v-text-field v-model="field.validationPattern" label="入力規則（正規表現）" variant="outlined" />
              <v-text-field
                v-if="field.fieldType === 'SELECT'"
                v-model="field.optionsJson"
                label="選択肢JSON"
                placeholder='["選択肢A", "選択肢B"]'
                variant="outlined"
              />
            </div>
            <div class="field-flags">
              <v-switch v-model="field.requiredFlag" label="必須" color="primary" />
              <v-switch v-model="field.listVisibleFlag" label="一覧表示" color="primary" />
              <v-switch v-model="field.searchableFlag" label="検索対象" color="primary" />
              <v-spacer />
              <v-btn color="error" variant="text" @click="removeField(index)">項目を削除</v-btn>
            </div>
          </v-card>
        </v-card-text>
        <v-card-actions>
          <v-btn v-if="editingId != null" color="error" variant="text" @click="remove">削除</v-btn>
          <v-btn v-if="editingId != null && editingStatus === 'DRAFT'" color="success" variant="tonal" @click="publish">公開</v-btn>
          <v-spacer />
          <v-btn variant="text" @click="dialog = false">キャンセル</v-btn>
          <v-btn color="primary" :loading="loading" @click="save">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.generic-master-panel {
  display: grid;
  gap: 16px;
}

.help-text {
  margin: 0;
  color: rgba(var(--v-theme-on-surface), 0.72);
}

.definition-grid,
.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 16px;
}

.field-heading,
.field-flags {
  display: flex;
  align-items: center;
  gap: 16px;
}

.field-heading {
  justify-content: space-between;
  margin-bottom: 12px;
}

.field-card {
  padding: 16px;
  margin-bottom: 12px;
  background: #fff;
}

@media (max-width: 720px) {
  .definition-grid,
  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
