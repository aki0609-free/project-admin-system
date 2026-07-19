import { computed, ref, watch, type Ref } from 'vue'
import { z } from 'zod'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type {
  BackupColumnForm,
  BackupTargetDialogForm,
} from '@/features/system/backup/types/backupFormTypes'
import { createEmptyBackupColumnForm } from '@/features/system/backup/utils/backupFormFactory'
import type { ToolbarItem } from '@/toolbox/toolbar/types/types'

export type BackupColumnTableRow = SimpleTableEditableRow & {
  id: number
  columnName: string
  csvHeaderName: string
  dataType: string
  exportText: string
  orderNo: number
  raw: BackupColumnForm
}

export const backupColumnSchema = z.object({
  id: z.number(),
  columnName: z.string().min(1, '必須です'),
  csvHeaderName: z.string().min(1, '必須です'),
  dataType: z.enum(['STRING', 'INTEGER', 'LONG', 'DECIMAL', 'BOOLEAN', 'DATE', 'DATETIME']),
  exportFlag: z.boolean(),
  orderNo: z.number().min(1, '必須です'),
})

export const useBackupColumnEditor = (formModel: BackupTargetDialogForm) => {
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
    { deep: true, immediate: true },
  )

  const rows = computed<BackupColumnTableRow[]>(() =>
    formModel.columns
      .slice()
      .sort((a, b) => a.orderNo - b.orderNo)
      .map((item) => ({
        id: item.id,
        columnName: item.columnName,
        csvHeaderName: item.csvHeaderName,
        dataType: item.dataType,
        exportText: item.exportFlag ? '出力' : '-',
        orderNo: item.orderNo,
        raw: item,
      })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<BackupColumnTableRow>[] = [
      { title: '順', key: 'orderNo', type: 'text', width: '80px', filter: { type: 'text' } },
      {
        title: 'columnName',
        key: 'columnName',
        type: 'text',
        width: '260px',
        filter: { type: 'text' },
      },
      {
        title: 'csvHeaderName',
        key: 'csvHeaderName',
        type: 'text',
        width: '260px',
        filter: { type: 'text' },
      },
      {
        title: 'dataType',
        key: 'dataType',
        type: 'text',
        width: '180px',
        filter: { type: 'text' },
      },
      {
        title: 'Export',
        key: 'exportText',
        type: 'text',
        width: '180px',
        filter: { type: 'text' },
      },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<BackupColumnTableRow>(columns.value),
  )

  const selectedColumnIndex = computed(() => {
    if (selectedColumnId.value == null) return -1
    return formModel.columns.findIndex((item) => item.id === selectedColumnId.value)
  })

  const selectedColumn = computed<BackupColumnForm | null>({
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

  const fields: GridFormFieldDef<BackupColumnForm>[] = [
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
    { key: 'exportFlag', label: 'exportFlag', type: 'checkbox', gridColumn: '1 / span 4' },
  ]

  const selectRow = (row: BackupColumnTableRow) => {
    selectedColumnId.value = row.id
  }

  const resequence = (items: BackupColumnForm[]) => {
    items.forEach((item, index) => {
      item.orderNo = index + 1
    })
    return items
  }

  const add = () => {
    const next = createEmptyBackupColumnForm(formModel.columns.length + 1)
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
    {
      type: 'button',
      label: '追加',
      color: 'primary',
      onClick: add,
    },
    {
      type: 'button',
      label: '↑',
      color: 'secondary',
      disabled: !hasSelection.value,
      onClick: moveUp,
    },
    {
      type: 'button',
      label: '↓',
      color: 'secondary',
      disabled: !hasSelection.value,
      onClick: moveDown,
    },
    {
      type: 'button',
      label: '削除',
      color: 'error',
      disabled: !hasSelection.value,
      onClick: remove,
    },
  ])

  return {
    rows,
    columns,
    filterRules,
    selectedColumn,
    fields,
    schema: backupColumnSchema,
    toolbarItems,
    selectRow,
    add,
    remove,
    moveUp,
    moveDown,
  }
}
