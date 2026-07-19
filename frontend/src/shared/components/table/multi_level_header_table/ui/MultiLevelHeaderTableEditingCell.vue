<script setup lang="ts" generic="T extends { id: number }">
import { computed, ref } from 'vue'
import type { MultiLevelHeaderLeafColumn } from '../types/item/types'

const props = defineProps<{
  item: T
  column: MultiLevelHeaderLeafColumn<T>
  editingCell: { id: number; key: string } | null
}>()

const emit = defineEmits<{
  (e: 'update:editingCell', v: { id: number; key: string } | null): void
  (e: 'update', payload: { id: number; field: keyof T; value: unknown }): void
}>()

const rowId = computed(() => props.item.id)
const menu = ref(false)

/* =========================
   表示値
========================= */

const displayValue = computed(() => {
  const val = props.item[props.column.key]

  // select
  if (props.column.type === 'select' && props.column.enumOptions) {
    const found = props.column.enumOptions.find(opt => opt.value === val)
    return found?.title ?? val
  }

  // formatter
  if (props.column.formatter) {
    return props.column.formatter(val, props.item)
  }

  return val ?? ''
})

/* =========================
   編集状態
========================= */

const isEditing = computed(() => {
  if (!props.editingCell) return false
  return (
    props.editingCell.id === rowId.value &&
    props.editingCell.key === String(props.column.key)
  )
})

/* =========================
   v-model
========================= */

const modelValue = computed({
  get: () => {
    const val = props.item[props.column.key]

    if (props.column.type === 'date' && typeof val === 'string') {
      return val
    }

    return val ?? ''
  },
  set: (v) =>
    emit('update', {
      id: props.item.id,
      field: props.column.key,
      value: v,
    }),
})

const onSelect = () => {
  stopEdit()
}

/* =========================
   checkbox
========================= */

const checkboxValue = computed<boolean>({
  get() {
    return props.item[props.column.key] === true
  },
  set(v) {
    emit('update', {
      id: props.item.id,
      field: props.column.key,
      value: v,
    })
  },
})

/* =========================
   イベント
========================= */

const startEdit = () => {
  if (!props.column.editable) return
  emit('update:editingCell', {
    id: rowId.value,
    key: String(props.column.key),
  })
}

const stopEdit = () => {
  requestAnimationFrame(() => {
    emit('update:editingCell', null)
  })
}

/* =========================
   required
========================= */

const isRequiredForEmpty = computed(() => {
  if (!props.column.required) return false
  const v = props.item[props.column.key]
  return v == null || v === ''
})

/* =========================
   date format
========================= */

const formattedDate = (val: string | Date | null) => {
  if (!val) return ''
  const date = typeof val === 'string' ? new Date(val) : val
  const yyyy = date.getFullYear()
  const mm = date.getMonth() + 1
  const dd = date.getDate()
  return `${yyyy}年${mm}月${dd}日`
}
</script>

<template>
  <div class="mlh-editable-cell" @click.stop.prevent="startEdit">
    <!-- =========================
         text / number
    ========================== -->
    <v-text-field
      v-if="isEditing && (column.type === 'text' || column.type === 'number' || !column.type)"
      v-model="modelValue"
      :type="column.type === 'number' ? 'number' : 'text'"
      density="compact"
      hide-details
      autofocus
      @blur="stopEdit"
      @keydown.enter="stopEdit"
    />

    <!-- =========================
         select
    ========================== -->
    <v-select
      v-else-if="isEditing && column.type === 'select'"
      v-model="modelValue"
      :items="column.enumOptions"
      item-title="title"
      item-value="value"
      density="compact"
      hide-details
      autofocus
      @update:model-value="onSelect"
    />

    <!-- =========================
         checkbox
    ========================== -->
    <v-checkbox
      v-else-if="column.type === 'checkbox'"
      :model-value="checkboxValue"
      @update:model-value="checkboxValue = $event ?? false"
      density="compact"
      hide-details
      @click.stop
    />

    <!-- =========================
         date
    ========================== -->
    <v-menu
      v-else-if="isEditing && column.type === 'date'"
      v-model="menu"
      :close-on-content-click="false"
      v-model:return-value="modelValue"
      transition="scale-transition"
      offset-y
      min-width="auto"
    >
      <template #activator="{ props: menuProps }">
        <v-text-field
          v-bind="menuProps"
          v-model="modelValue"
          readonly
          density="compact"
          hide-details
          autofocus
        />
      </template>
      <v-date-picker
        v-model="modelValue"
        @update:model-value="stopEdit"
      />
    </v-menu>

    <!-- =========================
         display
    ========================== -->
    <span v-else>
      <!-- date -->
      <template v-if="column.type === 'date'">
        <span class="cell-placeholder" :class="{ required: isRequiredForEmpty }">
          {{ formattedDate(modelValue as string | Date) }}
        </span>
      </template>

      <!-- default -->
      <template v-else>
        <span class="cell-placeholder" :class="{ required: isRequiredForEmpty }">
          {{ displayValue }}
        </span>
      </template>
    </span>
  </div>
</template>
