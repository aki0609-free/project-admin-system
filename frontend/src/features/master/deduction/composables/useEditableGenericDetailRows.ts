import { computed, ref } from 'vue'

export type EditableGenericDetailRow = {
  id: number
  deleteSelected?: boolean
  _isNew?: boolean
  _isUpdated?: boolean
  _isDeleted?: boolean
  [key: string]: unknown
}

type UpdatePayload = {
  id: number
  field: string | number
  value: unknown
}

export function useEditableGenericDetailRows(
  initialRows: EditableGenericDetailRow[] = [],
) {
  const rows = ref<EditableGenericDetailRow[]>([])

  function reset(value: EditableGenericDetailRow[]) {
    rows.value = value.map(item => ({
      ...item,
      deleteSelected: false,
      _isNew: false,
      _isUpdated: false,
      _isDeleted: false,
    }))
  }

  function addRow(row: EditableGenericDetailRow) {
    rows.value = [
      ...rows.value,
      {
        ...row,
        deleteSelected: false,
        _isNew: true,
        _isUpdated: true,
        _isDeleted: false,
      },
    ]
  }

  function updateCell(payload: UpdatePayload) {
    rows.value = rows.value.map(row => {
      if (row.id !== payload.id) return row

      return {
        ...row,
        [String(payload.field)]: payload.value,
        _isUpdated: true,
      }
    })
  }

  function markDeleted(id: number) {
    rows.value = rows.value.map(row => {
      if (row.id !== id) return row

      return {
        ...row,
        deleteSelected: false,
        _isDeleted: true,
        _isUpdated: false,
      }
    })
  }

  function markSelectedDeleted() {
    rows.value
      .filter(row => row.deleteSelected)
      .forEach(row => markDeleted(row.id))
  }

  const visibleRows = computed(() =>
    rows.value.filter(row => !row._isDeleted),
  )

  const changedRows = computed(() =>
    rows.value.filter(row => row._isNew || row._isUpdated || row._isDeleted),
  )

  const hasDeleteSelected = computed(() =>
    visibleRows.value.some(row => row.deleteSelected),
  )

  reset(initialRows)

  return {
    rows,
    visibleRows,
    changedRows,
    hasDeleteSelected,
    reset,
    addRow,
    updateCell,
    markDeleted,
    markSelectedDeleted,
  }
}