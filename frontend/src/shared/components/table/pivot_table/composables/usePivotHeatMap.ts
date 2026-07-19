import { computed, Ref } from 'vue'

export function usePivotHeatmap(matrix: Ref<(number | null)[][]>) {
  return computed(() => {
    const flat = matrix.value.flat().filter(v => v != null) as number[]

    const min = flat.length ? Math.min(...flat) : 0
    const max = flat.length ? Math.max(...flat) : 0

    const getColor = (value: number | null | undefined) => {
      if (value == null) return ''

      if (max === min) return 'rgba(59,130,246,0.15)'

      const ratio = (value - min) / (max - min)
      const alpha = 0.08 + ratio * 0.65

      return `rgba(59,130,246,${alpha})`
    }

    return { getColor }
  })
}