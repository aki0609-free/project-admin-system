<script setup lang="ts">
const props = defineProps<{
  rowKeys: string[]
  columnKeys: string[]
  matrix: number[][]
  rowTotals: number[]
  sortedIndexes: number[]
  showTotals: boolean
  getColor: (v: number | null | undefined) => string
}>()

const emit = defineEmits<{
  (e: 'cell-click', payload: { row: string; col: string; value: number | undefined }): void
}>()

function format(v: number | null | undefined) {
  return v == null ? '' : v.toLocaleString()
}
</script>

<template>
  <tbody>
    <tr
      v-for="rIndex in sortedIndexes"
      :key="rowKeys[rIndex]"
      class="row"
    >
      <th class="row-header sticky-left">
        {{ rowKeys[rIndex] }}
      </th>

      <td
        v-for="(col, cIndex) in columnKeys"
        :key="col"
        class="cell clickable"
        :style="{ backgroundColor: getColor(matrix[rIndex]?.[cIndex]) }"
        @click="emit('cell-click', {
          row: rowKeys[rIndex] ?? '',
          col,
          value: matrix[rIndex]?.[cIndex]
        })"
      >
        {{ format(matrix[rIndex]?.[cIndex]) }}
      </td>

      <td v-if="showTotals" class="cell total-cell">
        {{ format(rowTotals[rIndex]) }}
      </td>
    </tr>
  </tbody>
</template>