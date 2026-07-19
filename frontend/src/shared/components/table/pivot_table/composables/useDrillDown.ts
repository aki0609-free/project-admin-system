import { ref } from 'vue'

export function useDrillDown<T extends Record<string, any>>() {
  const dialog = ref(false)
  const data = ref<T[]>([])

  function open(
    source: T[],
    rowKey: keyof T,
    colKey: keyof T,
    rowValue: string,
    colValue: string
  ) {
    data.value = source.filter(item =>
      String(item[rowKey]) === rowValue &&
      String(item[colKey]) === colValue
    )

    dialog.value = true
  }

  function close() {
    dialog.value = false
  }

  return {
    dialog,
    data,
    open,
    close,
  }
}