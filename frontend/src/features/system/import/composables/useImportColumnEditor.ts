import { computed, ref, watch } from 'vue'
import { z } from 'zod'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type { ImportColumnForm, ImportTargetDialogForm } from '@/features/system/import/types/importFormTypes'
import type { ImportDataType } from '@/features/system/import/types/importApiTypes'
import { createEmptyImportColumn } from '@/features/system/import/utils/importFormFactory'

export type ImportColumnTableRow = SimpleTableEditableRow & {
  id: number
  orderNo: number
  columnName: string
  csvHeaderName: string
  dataType: ImportDataType
  keyText: string
  requiredText: string
  nullableText: string
  updatableText: string
  raw: ImportColumnForm
}

export const importColumnSchema = z.object({
  id: z.number(),
  columnName: z.string().min(1, '必須です'),
  csvHeaderName: z.string().min(1, '必須です'),
  dataType: z.enum(['STRING', 'INTEGER', 'LONG', 'DECIMAL', 'BOOLEAN', 'DATE', 'DATETIME']),
  requiredFlag: z.boolean(),
  keyFlag: z.boolean(),
  nullableFlag: z.boolean(),
  trimFlag: z.boolean(),
  defaultValue: z.string(),
  formatPattern: z.string(),
  updatableFlag: z.boolean(),
  orderNo: z.number().min(1),
})

export const useImportColumnEditor = (
  formModel: ImportTargetDialogForm,
) => {
  const selectedColumnId = ref<number | null>(null)

  watch(
    () => formModel.columns,
    (columns) => {
      if (!columns.length) {
        selectedColumnId.value = null
        return
      }

      const exists = columns.some((item) => item.id === selectedColumnId.value)
      if (!exists) {
        selectedColumnId.value = columns[0]?.id ?? null
      }
    },
    { immediate: true, deep: true },
  )

  const rows = computed<ImportColumnTableRow[]>(() =>
    formModel.columns
      .slice()
      .sort((a, b) => a.orderNo - b.orderNo)
      .map((item) => ({
        id: item.id,
        orderNo: item.orderNo,
        columnName: item.columnName,
        csvHeaderName: item.csvHeaderName,
        dataType: item.dataType,
        keyText: item.keyFlag ? 'KEY' : '-',
        requiredText: item.requiredFlag ? '必須' : '任意',
        nullableText: item.nullableFlag ? 'NULL可' : 'NULL不可',
        updatableText: item.updatableFlag ? '更新可' : '-',
        raw: item,
      })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<ImportColumnTableRow>[] = [
      { title: '順', key: 'orderNo', type: 'text', width: '80px', filter: { type: 'text' } },
      { title: 'columnName', key: 'columnName', type: 'text', width: '180px', filter: { type: 'text' } },
      { title: 'csvHeaderName', key: 'csvHeaderName', type: 'text', width: '180px', filter: { type: 'text' } },
      { title: 'dataType', key: 'dataType', type: 'text', width: '120px', filter: { type: 'text' } },
      { title: 'Key', key: 'keyText', type: 'text', width: '80px', filter: { type: 'text' } },
      { title: '必須', key: 'requiredText', type: 'text', width: '100px', filter: { type: 'text' } },
      { title: 'NULL', key: 'nullableText', type: 'text', width: '100px', filter: { type: 'text' } },
      { title: '更新', key: 'updatableText', type: 'text', width: '100px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<ImportColumnTableRow>(columns.value),
  )

  const selectedColumnIndex = computed(() => {
    if (selectedColumnId.value == null) return -1
    return formModel.columns.findIndex((item) => item.id === selectedColumnId.value)
  })

  const selectedColumn = computed<ImportColumnForm | null>({
    get: () => {
      const index = selectedColumnIndex.value
      if (index < 0) return null
      return formModel.columns[index] ?? null
    },
    set: (value) => {
      const index = selectedColumnIndex.value
      if (index < 0 || !value) return

      const next = [...formModel.columns]
      next[index] = value
      formModel.columns = next
    },
  })

  const fields: GridFormFieldDef<ImportColumnForm>[] = [
    { key: 'columnName', label: 'columnName', type: 'text', gridColumn: '1 / span 4' },
    { key: 'csvHeaderName', label: 'csvHeaderName', type: 'text', gridColumn: '1 / span 4' },
    {
      key: 'dataType',
      label: 'dataType',
      type: 'select',
      options: [
        { title: 'STRING', value: 'STRING' },
        { title: 'INTEGER', value: 'INTEGER' },
        { title: 'LONG', value: 'LONG' },
        { title: 'DECIMAL', value: 'DECIMAL' },
        { title: 'BOOLEAN', value: 'BOOLEAN' },
        { title: 'DATE', value: 'DATE' },
        { title: 'DATETIME', value: 'DATETIME' },
      ],
      gridColumn: '1 / span 4'
    },
    { key: 'orderNo', label: 'orderNo', type: 'number', gridColumn: '1 / span 4' },
    { key: 'defaultValue', label: 'defaultValue', type: 'text', gridColumn: '1 / span 4' },
    { key: 'formatPattern', label: 'formatPattern', type: 'text', gridColumn: '1 / span 4' },
    { key: 'requiredFlag', label: 'required', type: 'checkbox', gridColumn: '1 / span 2' },
    { key: 'keyFlag', label: 'key', type: 'checkbox', gridColumn: '3 / span 2' },
    { key: 'nullableFlag', label: 'nullable', type: 'checkbox', gridColumn: '1 / span 2'},
    { key: 'trimFlag', label: 'trim', type: 'checkbox', gridColumn: '3 / span 2' },
    { key: 'updatableFlag', label: 'updatable', type: 'checkbox', gridColumn: '1 / span 4' },
  ]

  const selectRow = (row: ImportColumnTableRow) => {
    selectedColumnId.value = row.id
  }

  const resequence = (items: ImportColumnForm[]) => {
    items.forEach((item, index) => {
      item.orderNo = index + 1
    })
    return items
  }

  const add = () => {
    const next = createEmptyImportColumn(formModel.columns.length + 1)
    formModel.columns = resequence([...formModel.columns, next])
    selectedColumnId.value = next.id
  }

  const remove = () => {
    const index = selectedColumnIndex.value
    if (index < 0) return

    const next = [...formModel.columns]
    next.splice(index, 1)

    formModel.columns = resequence(next)
    selectedColumnId.value = formModel.columns[0]?.id ?? null
  }

  const moveUp = () => {
    const index = selectedColumnIndex.value
    if (index <= 0) return

    const list = [...formModel.columns]
    const current = list[index]
    const prev = list[index - 1]

    if (!current || !prev) return

    list[index - 1] = current
    list[index] = prev

    formModel.columns = resequence(list)
    selectedColumnId.value = current.id
  }

  const moveDown = () => {
    const index = selectedColumnIndex.value
    if (index < 0 || index >= formModel.columns.length - 1) return

    const list = [...formModel.columns]
    const current = list[index]
    const next = list[index + 1]

    if (!current || !next) return

    list[index] = next
    list[index + 1] = current

    formModel.columns = resequence(list)
    selectedColumnId.value = current.id
  }

  const hasSelection = computed(() => selectedColumn.value != null)

  const toolbarItems = computed<ToolbarItem[]>(() => [
    { type: 'button', label: '追加', color: 'primary', onClick: add },
    { type: 'button', label: '↑', color: 'secondary', disabled: !hasSelection.value, onClick: moveUp },
    { type: 'button', label: '↓', color: 'secondary', disabled: !hasSelection.value, onClick: moveDown },
    { type: 'button', label: '削除', color: 'error', disabled: !hasSelection.value, onClick: remove },
  ])

  return {
    rows,
    columns,
    filterRules,
    selectedColumn,
    fields,
    schema: importColumnSchema,
    toolbarItems,
    selectRow,
  }
}