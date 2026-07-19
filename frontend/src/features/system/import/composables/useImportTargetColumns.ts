import { computed, ref } from 'vue'
import type { ImportDataType } from '@/features/system/import/types/importApiTypes'

export type ImportColumnForm = {
  id: number
  columnName: string
  csvHeaderName: string
  dataType: ImportDataType
  requiredFlag: boolean
  keyFlag: boolean
  nullableFlag: boolean
  trimFlag: boolean
  defaultValue: string
  formatPattern: string
  updatableFlag: boolean
  orderNo: number
}

let tempId = -1
const nextTempId = () => tempId--

export const createEmptyImportColumn = (orderNo = 1): ImportColumnForm => ({
  id: nextTempId(),
  columnName: '',
  csvHeaderName: '',
  dataType: 'STRING',
  requiredFlag: false,
  keyFlag: false,
  nullableFlag: true,
  trimFlag: true,
  defaultValue: '',
  formatPattern: '',
  updatableFlag: true,
  orderNo,
})

export const useImportTargetColumns = (
  getColumns: () => ImportColumnForm[],
  setColumns: (columns: ImportColumnForm[]) => void,
) => {
  const selectedColumnId = ref<number | null>(null)

  const selectedColumnIndex = computed(() => {
    if (selectedColumnId.value == null) return -1
    return getColumns().findIndex((item) => item.id === selectedColumnId.value)
  })

  const selectedColumn = computed<ImportColumnForm | null>(() => {
    const index = selectedColumnIndex.value
    if (index < 0) return null
    return getColumns()[index] ?? null
  })

  const ensureSelection = () => {
    const columns = getColumns()
    if (!columns.length) {
      selectedColumnId.value = null
      return
    }

    if (!columns.some((item) => item.id === selectedColumnId.value)) {
      selectedColumnId.value = columns[0]?.id ?? null
    }
  }

  const selectColumnId = (id: number) => {
    selectedColumnId.value = id
  }

  const addColumn = () => {
    const columns = getColumns()
    const next = createEmptyImportColumn(columns.length + 1)
    setColumns([...columns, next])
    selectedColumnId.value = next.id
  }

  const removeColumn = () => {
    const index = selectedColumnIndex.value
    if (index < 0) return

    const next = [...getColumns()]
    next.splice(index, 1)
    next.forEach((item, idx) => {
      item.orderNo = idx + 1
    })

    setColumns(next)
    selectedColumnId.value = next[0]?.id ?? null
  }

  const moveColumnUp = () => {
    const index = selectedColumnIndex.value
    if (index <= 0) return

    const list = [...getColumns()]
    const current = list[index]
    const prev = list[index - 1]

    if (!current || !prev) return

    list[index - 1] = current
    list[index] = prev

    list.forEach((item, idx) => {
      item.orderNo = idx + 1
    })

    setColumns(list)
    selectedColumnId.value = current.id
  }

  const moveColumnDown = () => {
    const index = selectedColumnIndex.value
    const list = [...getColumns()]

    if (index < 0 || index >= list.length - 1) return

    const current = list[index]
    const next = list[index + 1]

    if (!current || !next) return

    list[index] = next
    list[index + 1] = current

    list.forEach((item, idx) => {
      item.orderNo = idx + 1
    })

    setColumns(list)
    selectedColumnId.value = current.id
  }

  return {
    selectedColumnId,
    selectedColumn,
    ensureSelection,
    selectColumnId,
    addColumn,
    removeColumn,
    moveColumnUp,
    moveColumnDown,
  }
}
