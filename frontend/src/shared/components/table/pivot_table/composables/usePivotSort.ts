import { computed, Ref } from 'vue'

export function usePivotSort(
  rowKeys: Ref<string[]>,
  rowTotals: Ref<number[]>,
  sortType: Ref<'none' | 'asc' | 'desc'>
) {
  return computed(() => {
    const indexes = rowKeys.value.map((_, i) => i)

    if (sortType.value === 'none') return indexes

    return [...indexes].sort((a, b) => {
      const va = rowTotals.value[a] ?? 0
      const vb = rowTotals.value[b] ?? 0

      return sortType.value === 'desc' ? vb - va : va - vb
    })
  })
}