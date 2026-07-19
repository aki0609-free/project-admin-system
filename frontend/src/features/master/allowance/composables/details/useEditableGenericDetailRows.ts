import { computed, ref } from 'vue'

export type EditableGenericDetailRow = {
  id: number
  _isUpdated?: boolean
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
      _isUpdated: false,
    }))
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

  const updatedRows = computed(() =>
    rows.value.filter(row => row._isUpdated),
  )

  reset(initialRows)

  return {
    rows,
    updatedRows,
    reset,
    updateCell,
  }
}