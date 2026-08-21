<!-- eslint-disable @typescript-eslint/no-dynamic-delete -->
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { z } from 'zod'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { BatchParameterDefinition, ToolbarItem } from '@/shared/ui/toolbar/types'
import AppDialog from './AppDialog.vue'

const props = defineProps<{
  modelValue: boolean
  item: Extract<ToolbarItem, { type: 'batch' }> | null
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'execute', value: { jobCode: string; params: Record<string, unknown> }): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const formModel = reactive<Record<string, unknown>>({})
const formLayoutRef = ref<{ validateAll: () => boolean } | null>(null)

const definitions = computed<BatchParameterDefinition[]>(() =>
  props.item?.parameterDefinitions ?? [],
)

const fields = computed<GridFormFieldDef<Record<string, unknown>>[]>(() =>
  definitions.value.map(def => ({
    key: def.key,
    label: def.label,
    type: def.type,
    options: def.options,
    gridColumn: def.gridColumn ?? '1 / span 4',
    width: def.width,
  })) as GridFormFieldDef<Record<string, unknown>>[],
)

const schema = computed(() => {
  const shape: Record<string, z.ZodTypeAny> = {}

  for (const def of definitions.value) {
    let rule: z.ZodTypeAny

    switch (def.type) {
      case 'number':
        rule = z.coerce.number()
        break
      case 'checkbox':
        rule = z.boolean()
        break
      default:
        rule = z.string()
        break
    }

    if (!def.required) {
      rule = rule.optional()
    } else if (def.type !== 'checkbox') {
      rule = rule.refine(
        value => value !== undefined && value !== null && String(value).trim() !== '',
        `${def.label}は必須です`,
      )
    }

    shape[def.key] = rule
  }

  return z.object(shape)
})

const resetForm = () => {
  Object.keys(formModel).forEach(key => delete formModel[key])

  const item = props.item
  if (!item) return

  Object.assign(formModel, item.defaultParams ?? {})

  for (const def of item.parameterDefinitions ?? []) {
    if (formModel[def.key] !== undefined) continue

    if (def.defaultValue !== undefined) {
      formModel[def.key] = def.defaultValue
    } else if (def.type === 'checkbox') {
      formModel[def.key] = false
    } else {
      formModel[def.key] = ''
    }
  }
}

watch(
  [() => props.item, () => props.modelValue],
  ([, isOpen]) => {
    if (isOpen) resetForm()
  },
  { immediate: true },
)

const execute = () => {
  if (!props.item) return
  if (!(formLayoutRef.value?.validateAll() ?? false)) return

  if (props.item.confirmMessage && !window.confirm(props.item.confirmMessage)) {
    return
  }

  emit('execute', {
    jobCode: props.item.jobCode,
    params: { ...formModel },
  })
}

const rightFooterItems = computed<ToolbarItem[]>(() => [
  {
    id: 'close-batch-dialog',
    type: 'button',
    label: '閉じる',
    intent: 'secondary',
    disabled: props.loading ?? false,
    onClick: () => {
      visible.value = false
    },
  },
  {
    id: 'execute-batch',
    type: 'button',
    label: '実行',
    intent: 'primary',
    loading: props.loading ?? false,
    disabled: !props.item || (props.loading ?? false),
    onClick: execute,
  },
])
</script>

<template>
  <AppDialog
    v-model="visible"
    :title="item?.label ?? 'バッチ実行'"
    size="md"
    :max-width="760"
    body-layout="stack"
    :right-footer-items="rightFooterItems"
  >
    <div v-if="item" class="summary">
      <strong>jobCode：</strong>{{ item.jobCode }}
    </div>

    <FormLayout ref="formLayoutRef" v-model="formModel" :schema="schema">
      <GridBasedForm
        v-if="fields.length > 0"
        v-model="formModel"
        :fields="fields"
      />

      <div v-else class="empty-params">
        入力パラメータはありません。
      </div>
    </FormLayout>
  </AppDialog>
</template>

<style scoped>
.summary {
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
}

.empty-params {
  padding: 16px;
  color: #64748b;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
}
</style>
