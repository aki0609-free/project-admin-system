<script setup lang="ts">
defineProps<{
  columnKeys: string[]
  columnTotals: number[]
  grandTotal: number
  showRowTotals: boolean
}>()

function format(v: number | null | undefined) {
  return v == null ? '' : v.toLocaleString()
}
</script>

<template>
  <tfoot>
    <tr class="total-row">
      <th class="row-header sticky-left">合計</th>

      <td
        v-for="(col, i) in columnKeys"
        :key="i"
        class="cell total-cell"
      >
        {{ format(columnTotals[i]) }}
      </td>

      <td v-if="showRowTotals" class="cell grand-total">
        {{ format(grandTotal) }}
      </td>
    </tr>
  </tfoot>
</template>
