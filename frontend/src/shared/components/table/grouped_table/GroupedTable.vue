<script setup lang="ts" generic="T">
import { computed, ref } from 'vue'

/* =========================
   型
========================= */

type Aggregation = 'sum' | 'avg' | 'count'

type Column<T> = {
  key: keyof T
  title: string
  formatter?: (value: unknown, row: T) => string
  aggregation?: Aggregation
}

type Def<T> = {
  row: {
    key: keyof T
    title: string
  }
  columns: Column<T>[]
}

/* =========================
   Props
========================= */

const props = defineProps<{
  items: T[]
  def: Def<T>
  groupBy?: keyof T
}>()

/* =========================
   フィルター後データ（既存と接続想定）
========================= */

const filteredItems = computed(() => props.items)

/* =========================
   Grouping
========================= */

const groupedItems = computed(() => {
  if (!props.groupBy) {
    return new Map<string, T[]>([['', filteredItems.value]])
  }

  const map = new Map<string, T[]>()

  for (const item of filteredItems.value) {
    const key = String(item[props.groupBy] ?? '未分類')

    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(item)
  }

  return map
})

/* =========================
   開閉
========================= */

const openGroups = ref<Record<string, boolean>>({})

const isGroupOpen = (g: string) => (g in openGroups.value ? openGroups.value[g] : true)

const toggleGroup = (g: string) => (openGroups.value[g] = !isGroupOpen(g))

/* =========================
   小計
========================= */

function calcAggregation(rows: T[], col: Column<T>) {
  if (!col.aggregation) return ''

  const values = rows.map((r) => Number(r[col.key] ?? 0))

  switch (col.aggregation) {
    case 'sum':
      return values.reduce((a, b) => a + b, 0)
    case 'avg':
      return values.length ? Math.round(values.reduce((a, b) => a + b, 0) / values.length) : 0
    case 'count':
      return rows.length
  }
}
</script>

<template>
  <div class="mlh-table-wrapper">
    <table class="mlh-table">
      <!-- =========================
           HEADER
      ========================== -->
      <thead>
        <tr>
          <th class="mlh-th mlh-row-header">
            {{ def.row.title }}
          </th>

          <th v-for="col in def.columns" :key="String(col.key)" class="mlh-th">
            {{ col.title }}
          </th>
        </tr>
      </thead>

      <!-- =========================
           BODY（🔥統合ポイント）
      ========================== -->
      <tbody>
        <template v-for="[group, rows] in groupedItems" :key="group">
          <!-- GROUP -->
          <tr v-if="group && props.groupBy" class="mlh-group-row" @click="toggleGroup(group)">
            <td class="mlh-td mlh-row-header" :colspan="def.columns.length + 1">
              <span class="mlh-toggle">
                {{ isGroupOpen(group) ? '▼' : '▶' }}
              </span>
              {{ group }}（{{ rows.length }}）
            </td>
          </tr>

          <!-- DATA -->
          <template v-if="!group || isGroupOpen(group)">
            <tr v-for="item in rows" :key="group + '-' + String(item[def.row.key])">
              <td class="mlh-td mlh-row-header">
                {{ item[def.row.key] }}
              </td>

              <td v-for="col in def.columns" :key="String(col.key)" class="mlh-td">
                {{ col.formatter ? col.formatter(item[col.key], item) : item[col.key] }}
              </td>
            </tr>

            <!-- SUBTOTAL -->
            <tr v-if="group && props.groupBy" class="mlh-subtotal-row">
              <td class="mlh-td mlh-row-header">小計</td>

              <td v-for="col in def.columns" :key="String(col.key)" class="mlh-td">
                {{
                  col.aggregation
                    ? col.formatter
                      ? col.formatter(calcAggregation(rows, col), {} as T)
                      : calcAggregation(rows, col)
                    : ''
                }}
              </td>
            </tr>
          </template>
        </template>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
/* =========================
   WRAPPER
========================= */
.mlh-table-wrapper {
  overflow: auto;
}

/* =========================
   TABLE
========================= */
.mlh-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: max-content;
}

/* =========================
   HEADER
========================= */
.mlh-th {
  border: 1px solid #ddd;
  padding: 8px;
  background: #fafafa;
  font-weight: 600;
  position: sticky;
  top: 0;
  z-index: 4;
}

/* =========================
   CELL
========================= */
.mlh-td {
  border: 1px solid #eee;
  padding: 6px 8px;
}

/* =========================
   ROW HEADER（左固定）
========================= */
.mlh-row-header {
  position: sticky;
  left: 0;
  background: #fff;
  z-index: 5;
  border-right: 2px solid #e5e7eb;
  white-space: nowrap;
}

/* =========================
   GROUP
========================= */
.mlh-group-row td {
  background: #eef2f7;
  font-weight: 600;
  cursor: pointer;
}

.mlh-group-row:hover td {
  background: #e2e8f0;
}

/* =========================
   SUBTOTAL
========================= */
.mlh-subtotal-row td {
  background: #f9fafb;
  font-weight: 600;
  border-top: 2px solid #ccc;
}

/* toggle */
.mlh-toggle {
  display: inline-block;
  width: 16px;
  margin-right: 4px;
}
</style>
