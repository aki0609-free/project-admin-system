<!-- eslint-disable vue/attributes-order -->
<template>
  <div
    class="editable-cell"
    :class="`overflow-${cellOverflow}`"
    :title="tooltipText"
    @click="handleClick"
  >
    <!-- text -->
    <v-text-field
      v-if="isEditing && props.column.type === 'text'"
      v-model="modelValue"
      density="compact"
      :id="`cell-${rowId}-${String(props.field)}`"
      hide-details
      autofocus
      @blur="stopEdit"
    />

    <!-- number -->
    <v-text-field
      v-else-if="isEditing && props.column.type === 'number'"
      :model-value="numberInputValue"
      type="number"
      inputmode="decimal"
      :min="props.column.min"
      :max="props.column.max"
      :step="props.column.step ?? 'any'"
      :suffix="props.column.suffix"
      density="compact"
      :id="`cell-${rowId}-${String(props.field)}`"
      hide-details
      autofocus
      @update:model-value="updateNumberValue"
      @blur="stopEdit"
    />

    <!-- select -->
    <v-select
      v-else-if="isEditing && props.column.type === 'select'"
      v-model="modelValue"
      :items="column.enumOptions"
      item-title="title"
      item-value="value"
      density="compact"
      :id="`cell-${rowId}-${String(props.field)}`"
      autofocus
      hide-details
      @click.stop
      @mousedown.stop
      @update:model-value="stopEdit"
    />

    <!-- checkbox -->
    <v-checkbox
      v-else-if="props.column.type === 'checkbox'"
      :model-value="checkboxValue"
      @update:model-value="checkboxValue = $event ?? false"
      density="compact"
      hide-details
      @click.stop
    />

    <!-- date -->
    <v-menu
      v-else-if="isEditing && props.column.type === 'date'"
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
          :id="`cell-${rowId}-${String(props.field)}`"
          hide-details
          autofocus
        />
      </template>
      <v-locale-provider locale="ja">
        <v-date-picker
          v-model="modelValue"
          title="日付を選択"
          header="日付を選択"
          @update:model-value="stopEdit"
        />
      </v-locale-provider>
    </v-menu>

    <!-- dayrule -->
    <div
      v-else-if="isEditing && props.column.type === 'dayrule'"
      @click.stop
    >
      <DayRuleField
        v-model="dayRuleValue"
      />

      <div class="d-flex justify-end mt-1">
        <v-btn
          size="x-small"
          variant="text"
          @click.stop="stopEdit"
        >
          確定
        </v-btn>
      </div>
    </div>

    <span v-else>
      <template v-if="props.column.type === 'date'">
        <span class="cell-placeholder" :class="{ required: isRequiredForEmpty }">
          {{ formattedDate(modelValue as string | Date) }}
        </span>
      </template>

      <template v-else>
        <span class="cell-placeholder" :class="{ required: isRequiredForEmpty }">
          {{ displayValue }}
        </span>
      </template>
    </span>
  </div>
</template>

<script setup lang="ts" generic="T extends { id: number }">
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { DayRule } from '@/shared/types/dayRuleTypes'
import DayRuleField from '@/shared/components/form/base/components/form/DayRuleField.vue'
import { computed, ref } from 'vue'

const props = defineProps<{
  item: T
  field: keyof T
  column: SimpleTableColumnDef<T>
  editingCell: { id: number; key: string } | null
}>()

const emit = defineEmits<{
  (e: 'update:editingCell', v: { id: number; key: string } | null): void
  (e: 'update', payload: { id: number; field: keyof T; value: unknown }): void
}>()

const rowId = computed(() => props.item.id)
const menu = ref(false)

const rawValue = computed(() => {
  if (props.column.valueGetter) {
    return props.column.valueGetter(props.item)
  }
  return props.item[props.field]
})

const displayValue = computed(() => {
  const val = rawValue.value

  if (props.column.type === 'select' && props.column.enumOptions) {
    const found = props.column.enumOptions.find(
      opt => opt.value === val,
    )
    return found?.title ?? val
  }

  if (props.column.formatter) {
    return props.column.formatter(val, props.item)
  }

  return val ?? ''
})

const cellOverflow = computed(() => props.column.overflow ?? 'ellipsis')

const displayText = computed(() => {
  if (props.column.type === 'date') {
    return formattedDate(rawValue.value as string | Date | null)
  }
  return String(displayValue.value ?? '')
})

const tooltipText = computed(() => {
  if (isEditing.value || cellOverflow.value !== 'ellipsis') return undefined
  return displayText.value || undefined
})

const isEditing = computed(() => {
  if (!props.editingCell) return false

  return props.editingCell?.id === rowId.value && props.editingCell.key === props.field
})

const modelValue = computed({
  get: () => {
    const val = rawValue.value

    if (props.column.type === 'date' && typeof val === 'string') {
      return val
    }

    return val ?? ''
  },
  set: (v) =>
    emit('update', {
      id: props.item.id,
      field: props.field,
      value: v,
    }),
})

const checkboxValue = computed<boolean>({
  get() {
    return props.item[props.field] === true
  },
  set(v) {
    emit('update', {
      id: props.item.id,
      field: props.field,
      value: v,
    })
  },
})

const numberInputValue = computed(() => {
  const value = rawValue.value
  return value == null || value === '' ? '' : Number(value)
})

const dayRuleValue = computed<DayRule | null>({
  get() {
    const value = rawValue.value
    return value == null || value === ''
      ? null
      : value as DayRule
  },
  set(value) {
    emit('update', {
      id: props.item.id,
      field: props.field,
      value,
    })
  },
})

const updateNumberValue = (value: string | number | null) => {
  const normalized = value == null || value === ''
    ? null
    : Number(value)

  emit('update', {
    id: props.item.id,
    field: props.field,
    value: Number.isNaN(normalized) ? null : normalized,
  })
}

const startEdit = () => {
  if (!props.column.editable) return
  emit('update:editingCell', { id: rowId.value, key: props.field as string })
}

const stopEdit = () => {
  requestAnimationFrame(() => {
    emit('update:editingCell', null)
  })
}

const handleClick = (e: MouseEvent) => {
  if (!props.column.editable || props.column.computed) return

  e.stopPropagation()
  startEdit()
}

const isRequiredForEmpty = computed(() => {
  if (!props.column.required) return false
  const v = props.item[props.field]
  return v == null || v === ''
})

const formattedDate = (val: string | Date | null) => {
  if (!val) return ''
  const date = typeof val === 'string' ? new Date(val) : val
  const yyyy = date.getFullYear()
  const mm = date.getMonth() + 1
  const dd = date.getDate()
  return `${yyyy}年${mm}月${dd}日`
}
</script>

<style lang="css" scoped>
.editable-cell {
  min-height: 24px;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}

.cell-placeholder {
  min-height: 24px;
  display: block;
  width: 100%;
  min-width: 0;
  max-width: 100%;
}

.overflow-ellipsis .cell-placeholder {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overflow-wrap {
  overflow: visible;
}

.overflow-wrap .cell-placeholder {
  overflow-wrap: anywhere;
  white-space: normal;
}

.overflow-visible {
  overflow: visible;
}

.overflow-visible .cell-placeholder {
  white-space: nowrap;
}

.required {
  background-color: #ffecec;
  border: 1px solid #ffb3b3;
}
</style>
