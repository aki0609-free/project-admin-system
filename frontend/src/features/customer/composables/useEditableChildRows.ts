import { computed, shallowRef } from 'vue'
import type { SimpleTableEditableRow } from '@/shared/components/table/simple_table/types/item/types'

type SelectableDeleteRow = SimpleTableEditableRow & {
  deleteSelected?: boolean
}

export const useEditableChildRows = <T extends SelectableDeleteRow>(
  initialRows: T[] = [],
) => {
  const rows = shallowRef<T[]>([...initialRows])

  const visibleRows = computed(() =>
    rows.value.filter(row => !row._isDeleted),
  )

  const hasDeleteSelected = computed(() =>
    visibleRows.value.some(row => row.deleteSelected),
  )

  const addRow = (row: T) => {
    rows.value = [
      ...rows.value,
      {
        ...row,
        deleteSelected: false,
        _isNew: true,
        _isUpdated: false,
        _isDeleted: false,
      },
    ]
  }

  const updateCell = (payload: {
    id: number
    field: keyof T
    value: unknown
  }) => {
    rows.value = rows.value.map(row => {
      if (row.id !== payload.id) return row

      return {
        ...row,
        [payload.field]: payload.value,
        _isUpdated:
          payload.field !== 'deleteSelected' && !row._isNew
            ? true
            : row._isUpdated,
      } as T
    })
  }

  const markDeleted = (id: number) => {
    const row = rows.value.find(item => item.id === id)
    if (!row) return

    if (row._isNew) {
      rows.value = rows.value.filter(item => item.id !== id)
      return
    }

    rows.value = rows.value.map(item => {
      if (item.id !== id) return item

      return {
        ...item,
        _isDeleted: true,
        _isUpdated: false,
        deleteSelected: false,
      } as T
    })
  }

  const markSelectedDeleted = () => {
    const selectedIds = visibleRows.value
      .filter(row => row.deleteSelected)
      .map(row => row.id)

    selectedIds.forEach(markDeleted)
  }

  const resetRows = (nextRows: T[]) => {
    rows.value = nextRows.map(row => ({
      ...row,
      deleteSelected: false,
    })) as T[]
  }

  return {
    rows,
    visibleRows,
    hasDeleteSelected,
    addRow,
    updateCell,
    markDeleted,
    markSelectedDeleted,
    resetRows,
  }
}