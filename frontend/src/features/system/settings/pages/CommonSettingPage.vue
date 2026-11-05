<script setup lang="ts">
import { computed, reactive, ref } from 'vue'

import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'
import AppToolbar from '@/shared/ui/toolbar/AppToolbar.vue'
import type { BatchParameterOption, ToolbarItem } from '@/shared/ui/toolbar/types'
import GenericMasterDefinitionPanel from '@/features/system/masterdata/components/GenericMasterDefinitionPanel.vue'

import { useCommonSettingPage } from '../composables/useCommonSettingPage'
import type {
  SystemSettingDefinition,
  SystemSettingDefinitionCreateRequest,
  SystemSettingValueType,
} from '../types/commonSettingTypes'

type SettingMasterRow = SystemSettingDefinition & {
  scopeLabel: string
  valueTypeLabel: string
  statusLabel: string
  displayValue: string
}

const {
  loading,
  errorMessage,
  successMessage,
  definitions,
  saveDefinitionValue,
  createDefinition,
  updateDefinition,
  deleteDefinition,
} = useCommonSettingPage()

const tab = ref('master')
const selectedLargeCategory = ref<string | null>(null)
const selectedMiddleCategory = ref<string | null>(null)
const selectedSmallCategory = ref<string | null>(null)
const definitionDialog = ref(false)
const editingDefinition = ref<SystemSettingDefinition | null>(null)

const valueTypeLabels: Record<SystemSettingValueType, string> = {
  STRING: '文字列',
  NUMBER: '数値',
  BOOLEAN: '真偽値',
  URL: 'HTTPS URL',
  DATE: '日付',
  JSON: 'JSON',
}

const valueTypes: Array<{ title: string, value: SystemSettingValueType }> =
  Object.entries(valueTypeLabels).map(([value, title]) => ({
    title,
    value: value as SystemSettingValueType,
  }))

const emptyDefinition = (): SystemSettingDefinitionCreateRequest => ({
  largeCategoryKey: '',
  largeCategoryName: '',
  middleCategoryKey: '',
  middleCategoryName: '',
  smallCategoryKey: '',
  settingName: '',
  description: '',
  valueType: 'STRING',
  requiredFlag: false,
  validationPattern: '',
  placeholder: '',
  displayOrder: 100,
  sensitiveFlag: false,
  activeFlag: true,
  settingValue: '',
})

const definitionForm = reactive<SystemSettingDefinitionCreateRequest>(emptyDefinition())

const masterRows = computed<SettingMasterRow[]>(() => definitions.value.map(definition => ({
  ...definition,
  scopeLabel: definition.scope === 'SYSTEM' ? '標準' : '会社独自',
  valueTypeLabel: valueTypeLabels[definition.valueType],
  statusLabel: definition.activeFlag ? '有効' : '無効',
  displayValue: definition.sensitiveFlag && definition.settingValue
    ? '••••••••'
    : definition.settingValue,
})))

const filteredRows = computed(() => masterRows.value.filter(row =>
  (!selectedLargeCategory.value || row.largeCategoryKey === selectedLargeCategory.value)
  && (!selectedMiddleCategory.value || row.middleCategoryKey === selectedMiddleCategory.value)
  && (!selectedSmallCategory.value || row.smallCategoryKey === selectedSmallCategory.value),
))

const uniqueOptions = (
  rows: SettingMasterRow[],
  valueKey: keyof SettingMasterRow,
  titleKey: keyof SettingMasterRow,
): BatchParameterOption[] => {
  const options = new Map<string, string>()
  for (const row of rows) {
    options.set(String(row[valueKey]), String(row[titleKey]))
  }
  return [...options.entries()]
    .sort((left, right) => left[1].localeCompare(right[1], 'ja'))
    .map(([value, title]) => ({ value, title }))
}

const largeCategoryOptions = computed(() =>
  uniqueOptions(masterRows.value, 'largeCategoryKey', 'largeCategoryName'),
)

const middleCategoryOptions = computed(() => uniqueOptions(
  masterRows.value.filter(row =>
    !selectedLargeCategory.value || row.largeCategoryKey === selectedLargeCategory.value,
  ),
  'middleCategoryKey',
  'middleCategoryName',
))

const smallCategoryOptions = computed(() => uniqueOptions(
  masterRows.value.filter(row =>
    (!selectedLargeCategory.value || row.largeCategoryKey === selectedLargeCategory.value)
    && (!selectedMiddleCategory.value || row.middleCategoryKey === selectedMiddleCategory.value),
  ),
  'smallCategoryKey',
  'settingName',
))

const normalizeSelection = (value: string | number | boolean | null) =>
  value == null || value === '' ? null : String(value)

const resetCategoryFilters = () => {
  selectedLargeCategory.value = null
  selectedMiddleCategory.value = null
  selectedSmallCategory.value = null
}

const leftToolbarItems = computed<ToolbarItem[]>(() => [
  {
    id: 'large-category',
    type: 'select',
    label: '大分類',
    modelValue: selectedLargeCategory.value,
    options: largeCategoryOptions.value,
    'onUpdate:modelValue': value => {
      selectedLargeCategory.value = normalizeSelection(value)
      selectedMiddleCategory.value = null
      selectedSmallCategory.value = null
    },
  },
  {
    id: 'middle-category',
    type: 'select',
    label: '中分類',
    modelValue: selectedMiddleCategory.value,
    options: middleCategoryOptions.value,
    disabled: !selectedLargeCategory.value,
    'onUpdate:modelValue': value => {
      selectedMiddleCategory.value = normalizeSelection(value)
      selectedSmallCategory.value = null
    },
  },
  {
    id: 'small-category',
    type: 'select',
    label: '小分類',
    modelValue: selectedSmallCategory.value,
    options: smallCategoryOptions.value,
    disabled: !selectedMiddleCategory.value,
    'onUpdate:modelValue': value => {
      selectedSmallCategory.value = normalizeSelection(value)
    },
  },
  {
    id: 'clear-categories',
    type: 'button',
    label: '条件クリア',
    intent: 'utility',
    disabled: !selectedLargeCategory.value
      && !selectedMiddleCategory.value
      && !selectedSmallCategory.value,
    onClick: resetCategoryFilters,
  },
])

const rightToolbarItems = computed<ToolbarItem[]>(() => [{
  id: 'create-setting',
  type: 'button',
  label: '設定項目を追加',
  intent: 'primary',
  onClick: () => openCreateDialog(),
}])

const columns = computed<SimpleTableColumnDef<SettingMasterRow>[]>(() => [
  {
    title: '区分',
    key: 'scopeLabel',
    width: '120px',
    filter: { type: 'select' },
    enumOptions: [
      { title: '標準', value: '標準' },
      { title: '会社独自', value: '会社独自' },
    ],
  },
  { title: '大分類', key: 'largeCategoryName', width: '190px', filter: { type: 'text' } },
  { title: '中分類', key: 'middleCategoryName', width: '190px', filter: { type: 'text' } },
  { title: '小分類', key: 'settingName', width: '220px', filter: { type: 'text' } },
  {
    title: '設定値',
    key: 'displayValue',
    width: '300px',
    filter: { type: 'text' },
  },
  {
    title: '値型',
    key: 'valueTypeLabel',
    width: '140px',
    filter: { type: 'select' },
    enumOptions: Object.values(valueTypeLabels).map(value => ({ title: value, value })),
  },
  {
    title: '状態',
    key: 'statusLabel',
    width: '110px',
    filter: { type: 'select' },
    enumOptions: [
      { title: '有効', value: '有効' },
      { title: '無効', value: '無効' },
    ],
  },
])

const filterRules = computed(() => createSimpleTableFilterRules(columns.value))
const isSystemDefinition = computed(() => editingDefinition.value?.scope === 'SYSTEM')

const openCreateDialog = () => {
  editingDefinition.value = null
  Object.assign(definitionForm, emptyDefinition())
  definitionDialog.value = true
}

const openEditDialog = (definition: SystemSettingDefinition) => {
  editingDefinition.value = definition
  Object.assign(definitionForm, {
    largeCategoryKey: definition.largeCategoryKey,
    largeCategoryName: definition.largeCategoryName,
    middleCategoryKey: definition.middleCategoryKey,
    middleCategoryName: definition.middleCategoryName,
    smallCategoryKey: definition.smallCategoryKey,
    settingName: definition.settingName,
    description: definition.description ?? '',
    valueType: definition.valueType,
    requiredFlag: definition.requiredFlag,
    validationPattern: definition.validationPattern ?? '',
    placeholder: definition.placeholder ?? '',
    displayOrder: definition.displayOrder,
    sensitiveFlag: definition.sensitiveFlag,
    activeFlag: definition.activeFlag,
    settingValue: definition.settingValue,
  })
  definitionDialog.value = true
}

const validateDefinition = () => {
  const keyPattern = /^[A-Z][A-Z0-9_]*$/
  if (!keyPattern.test(definitionForm.largeCategoryKey)
    || !keyPattern.test(definitionForm.middleCategoryKey)
    || !keyPattern.test(definitionForm.smallCategoryKey)) {
    errorMessage.value = '分類キーは半角英大文字・数字・アンダースコアで入力してください。'
    return false
  }
  if (!definitionForm.largeCategoryName.trim()
    || !definitionForm.middleCategoryName.trim()
    || !definitionForm.settingName.trim()) {
    errorMessage.value = '分類名と設定項目名は必須です。'
    return false
  }
  if (definitionForm.requiredFlag && !definitionForm.settingValue.trim()) {
    errorMessage.value = `${definitionForm.settingName}は必須です。`
    return false
  }
  if (definitionForm.valueType === 'URL' && definitionForm.settingValue) {
    try {
      const url = new URL(definitionForm.settingValue)
      if (url.protocol !== 'https:') throw new Error()
    } catch {
      errorMessage.value = `${definitionForm.settingName}には有効なHTTPS URLを指定してください。`
      return false
    }
  }
  if (definitionForm.valueType === 'JSON' && definitionForm.settingValue) {
    try {
      JSON.parse(definitionForm.settingValue)
    } catch {
      errorMessage.value = `${definitionForm.settingName}には有効なJSONを指定してください。`
      return false
    }
  }
  return true
}

const submitDefinition = async () => {
  if (!validateDefinition()) return
  if (!editingDefinition.value) {
    await createDefinition({ ...definitionForm })
  } else if (isSystemDefinition.value) {
    await saveDefinitionValue(editingDefinition.value.id, definitionForm.settingValue)
  } else {
    await updateDefinition(editingDefinition.value.id, {
      largeCategoryName: definitionForm.largeCategoryName,
      middleCategoryName: definitionForm.middleCategoryName,
      settingName: definitionForm.settingName,
      description: definitionForm.description,
      valueType: definitionForm.valueType,
      requiredFlag: definitionForm.requiredFlag,
      validationPattern: definitionForm.validationPattern,
      placeholder: definitionForm.placeholder,
      displayOrder: definitionForm.displayOrder,
      sensitiveFlag: definitionForm.sensitiveFlag,
      activeFlag: definitionForm.activeFlag,
      settingValue: definitionForm.settingValue,
    })
  }
  if (!errorMessage.value) definitionDialog.value = false
}

const confirmDelete = async () => {
  const definition = editingDefinition.value
  if (!definition?.editable) return
  if (!window.confirm(`「${definition.settingName}」を削除しますか？`)) return
  await deleteDefinition(definition.id)
  if (!errorMessage.value) definitionDialog.value = false
}
</script>

<template>
  <ListDetailPageLayout
    title="システム共通設定"
    description="複数の業務機能から共通で参照する設定項目と設定値を管理します。"
  >
    <v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      closable
      @click:close="errorMessage = ''"
    >
      {{ errorMessage }}
    </v-alert>

    <v-alert
      v-if="successMessage"
      type="success"
      variant="tonal"
      closable
      @click:close="successMessage = ''"
    >
      {{ successMessage }}
    </v-alert>

    <v-card variant="outlined" class="tab-card">
      <v-tabs v-model="tab" color="primary">
        <v-tab value="master">設定値マスター</v-tab>
        <v-tab value="genericMaster">汎用マスター定義</v-tab>
      </v-tabs>
    </v-card>

    <v-progress-linear v-if="loading" indeterminate color="primary" />

    <v-window v-model="tab">
      <v-window-item value="master" class="master-content">
        <AppToolbar
          :left-items="leftToolbarItems"
          :right-items="rightToolbarItems"
          surface="page"
        />

        <p class="master-help">
          標準項目は設定値のみ変更できます。会社独自項目は追加・編集・削除できます。
        </p>

        <SimpleTable
          table-key="system-common-setting-master"
          item-key="id"
          :items="filteredRows"
          :columns="columns"
          :filter-rules="filterRules"
          :enable-row-click="true"
          @row-click="openEditDialog"
        />
      </v-window-item>
      <v-window-item value="genericMaster" class="master-content">
        <GenericMasterDefinitionPanel />
      </v-window-item>
    </v-window>

    <v-dialog v-model="definitionDialog" max-width="900">
      <v-card v-if="definitionDialog">
        <v-card-title>
          {{ editingDefinition ? '設定値を編集' : '会社独自の設定項目を追加' }}
        </v-card-title>
        <v-card-text>
          <v-alert type="info" variant="tonal" class="mb-5">
            <template v-if="isSystemDefinition">
              標準項目の定義は保護されています。会社ごとの設定値のみ変更できます。
            </template>
            <template v-else>
              分類キーは登録後に変更できません。業務処理から参照する安定した英大文字キーを指定してください。
            </template>
          </v-alert>

          <div class="definition-grid">
            <v-text-field v-model="definitionForm.largeCategoryKey" label="大分類キー" :readonly="Boolean(editingDefinition)" variant="outlined" />
            <v-text-field v-model="definitionForm.largeCategoryName" label="大分類名" :readonly="isSystemDefinition" variant="outlined" />
            <v-text-field v-model="definitionForm.middleCategoryKey" label="中分類キー" :readonly="Boolean(editingDefinition)" variant="outlined" />
            <v-text-field v-model="definitionForm.middleCategoryName" label="中分類名" :readonly="isSystemDefinition" variant="outlined" />
            <v-text-field v-model="definitionForm.smallCategoryKey" label="小分類キー" :readonly="Boolean(editingDefinition)" variant="outlined" />
            <v-text-field v-model="definitionForm.settingName" label="設定項目名" :readonly="isSystemDefinition" variant="outlined" />
            <v-select v-model="definitionForm.valueType" label="値型" :items="valueTypes" :readonly="isSystemDefinition" variant="outlined" />
            <v-text-field v-model.number="definitionForm.displayOrder" label="表示順" type="number" :readonly="isSystemDefinition" variant="outlined" />
            <v-text-field v-model="definitionForm.placeholder" label="入力例" :readonly="isSystemDefinition" variant="outlined" />
            <v-text-field v-model="definitionForm.validationPattern" label="入力規則（正規表現）" :readonly="isSystemDefinition" variant="outlined" />
          </div>

          <v-textarea v-model="definitionForm.description" label="説明" :readonly="isSystemDefinition" variant="outlined" rows="2" />

          <v-select
            v-if="definitionForm.valueType === 'BOOLEAN'"
            v-model="definitionForm.settingValue"
            label="設定値"
            :items="[{ title: '有効', value: 'true' }, { title: '無効', value: 'false' }]"
            variant="outlined"
          />
          <v-textarea
            v-else-if="definitionForm.valueType === 'JSON'"
            v-model="definitionForm.settingValue"
            label="設定値"
            :placeholder="definitionForm.placeholder || undefined"
            variant="outlined"
            rows="5"
          />
          <v-text-field
            v-else
            v-model="definitionForm.settingValue"
            label="設定値"
            :type="definitionForm.sensitiveFlag ? 'password'
              : definitionForm.valueType === 'NUMBER' ? 'number'
                : definitionForm.valueType === 'DATE' ? 'date' : 'text'"
            :placeholder="definitionForm.placeholder || undefined"
            variant="outlined"
          />

          <div class="flag-row">
            <v-switch v-model="definitionForm.requiredFlag" label="必須" color="primary" :readonly="isSystemDefinition" />
            <v-switch v-model="definitionForm.sensitiveFlag" label="機密値" color="primary" :readonly="isSystemDefinition" />
            <v-switch v-model="definitionForm.activeFlag" label="有効" color="primary" :readonly="isSystemDefinition" />
          </div>
        </v-card-text>
        <v-card-actions>
          <v-btn v-if="editingDefinition?.editable" color="error" variant="text" :disabled="loading" @click="confirmDelete">
            削除
          </v-btn>
          <v-spacer />
          <v-btn variant="text" @click="definitionDialog = false">キャンセル</v-btn>
          <v-btn color="primary" :loading="loading" @click="submitDefinition">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </ListDetailPageLayout>
</template>

<style scoped>
.tab-card {
  background: #fff;
  border-color: rgba(var(--v-theme-on-surface), 0.28);
  border-radius: 12px;
  box-shadow: none;
}

.master-content {
  display: grid;
  gap: 16px;
}

.master-help {
  margin: 0;
  color: rgba(var(--v-theme-on-surface), 0.72);
  font-size: 0.9rem;
}

.definition-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 16px;
}

.flag-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
}

@media (max-width: 720px) {
  .definition-grid {
    grid-template-columns: 1fr;
  }
}
</style>
