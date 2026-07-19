import { computed, reactive, ref, watch, type Ref } from 'vue'
import { z } from 'zod'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type { ImportTargetResponse } from '@/features/system/import/types/importApiTypes'
import type { ImportTargetDialogForm } from '@/features/system/import/types/importFormTypes'
import {
  createEmptyImportTargetForm,
  toImportTargetDialogForm,
} from '@/features/system/import/utils/importFormFactory'

export const importTargetSchema = z.object({
  id: z.number(),
  targetCode: z.string().min(1, '必須です'),
  targetName: z.string().min(1, '必須です'),
  tableName: z.string().min(1, '必須です'),
  description: z.string(),
  sourceType: z.enum(['UPLOAD', 'SERVER_FILE', 'SCRIPT']),
  fixedFilePath: z.string(),
  scriptType: z.enum(['NONE', 'SHELL', 'PYTHON']),
  scriptPath: z.string(),
  scriptArgs: z.string(),
  importMode: z.enum(['INSERT_ONLY', 'UPDATE_ONLY', 'UPSERT']),
  headerRowNumber: z.number().min(1),
  dataStartRowNumber: z.number().min(1),
  charset: z.string().min(1),
  delimiter: z.string().min(1),
  activeFlag: z.boolean(),
  columns: z.array(z.any()),
})

export const useImportTargetEditDialog = (
  visible: Ref<boolean>,
  target: Ref<ImportTargetResponse | null>,
  emitSave: (form: ImportTargetDialogForm) => void,
  emitDelete: (form: ImportTargetDialogForm) => void,
) => {
  const activeTab = ref<'basic' | 'columns'>('basic')
  const formModel = reactive<ImportTargetDialogForm>(createEmptyImportTargetForm())

  const resetForm = () => {
    Object.assign(formModel, createEmptyImportTargetForm())
    activeTab.value = 'basic'
  }

  watch(
    () => target.value,
    (value) => {
      if (!visible.value) return

      if (!value) {
        resetForm()
        return
      }

      Object.assign(formModel, toImportTargetDialogForm(value))
      activeTab.value = 'basic'
    },
    { immediate: true },
  )

  watch(
    () => visible.value,
    (opened) => {
      if (!opened) return
      if (!target.value) resetForm()
    },
  )

  const isEdit = computed(() => formModel.id > 0)

  const tabs = [
    { label: '基本情報', value: 'basic' },
    { label: 'Column', value: 'columns' },
  ]

  const basicFields: GridFormFieldDef<ImportTargetDialogForm>[] = [
    { key: 'id', label: 'ID', type: 'number', gridColumn: '1 / span 1' },
    { key: 'targetCode', label: 'targetCode', type: 'text', gridColumn: '2 / span 3' },
    { key: 'targetName', label: 'targetName', type: 'text', gridColumn: '1 / span 2' },
    { key: 'tableName', label: 'tableName', type: 'text', gridColumn: '3 / span 2' },
    {
      key: 'sourceType',
      label: 'sourceType',
      type: 'select',
      options: [
        { title: 'UPLOAD', value: 'UPLOAD' },
        { title: 'SERVER_FILE', value: 'SERVER_FILE' },
        { title: 'SCRIPT', value: 'SCRIPT' },
      ],
    },
    {
      key: 'scriptType',
      label: 'scriptType',
      type: 'select',
      options: [
        { title: 'NONE', value: 'NONE' },
        { title: 'SHELL', value: 'SHELL' },
        { title: 'PYTHON', value: 'PYTHON' },
      ],
    },
    {
      key: 'importMode',
      label: 'importMode',
      type: 'select',
      options: [
        { title: 'INSERT_ONLY', value: 'INSERT_ONLY' },
        { title: 'UPDATE_ONLY', value: 'UPDATE_ONLY' },
        { title: 'UPSERT', value: 'UPSERT' },
        { title: 'DELETE_INSERT', value: 'DELETE_INSERT' },
      ],
    },
    { key: 'activeFlag', label: '有効', type: 'checkbox', width: 100 },
    { key: 'fixedFilePath', label: 'fixedFilePath', type: 'text', gridColumn: '1 / span 4' },
    { key: 'scriptPath', label: 'scriptPath', type: 'text', gridColumn: '1 / span 4' },
    { key: 'scriptArgs', label: 'scriptArgs', type: 'text', gridColumn: '1 / span 4' },
    { key: 'headerRowNumber', label: 'headerRow', type: 'number' },
    { key: 'dataStartRowNumber', label: 'dataStartRow', type: 'number' },
    { key: 'charset', label: 'charset', type: 'text' },
    { key: 'delimiter', label: 'delimiter', type: 'text' },
  ]

  const save = () => {
    emitSave({
      ...formModel,
      columns: formModel.columns
        .slice()
        .sort((a, b) => a.orderNo - b.orderNo)
        .map((item) => ({ ...item })),
    })
  }

  const remove = () => {
    emitDelete({
      ...formModel,
      columns: formModel.columns.map((item) => ({ ...item })),
    })
  }

  const close = () => {
    visible.value = false
  }

  const footerItems = computed<ToolbarItem[]>(() => {
    const items: ToolbarItem[] = []

    if (isEdit.value) {
      items.push({
        type: 'button',
        label: '削除',
        color: 'error',
        onClick: remove,
      })
    }

    items.push(
      {
        type: 'button',
        label: '閉じる',
        color: 'secondary',
        onClick: close,
      },
      {
        type: 'button',
        label: '保存',
        color: 'primary',
        onClick: save,
      },
    )

    return items
  })

  return {
    activeTab,
    formModel,
    isEdit,
    tabs,
    basicFields,
    schema: importTargetSchema,
    footerItems,
  }
}