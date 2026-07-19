<!-- eslint-disable @typescript-eslint/no-dynamic-delete -->
<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { z } from 'zod'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import type {
  BatchParameterDefinition,
  ToolbarItem,
} from '@/shared/components/toolbar/types/types'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'

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

const definitions = computed<BatchParameterDefinition[]>(() =>
  props.item?.parameterDefinitions ?? [],
)

const fields = computed<GridFormFieldDef<Record<string, unknown>>[]>(() =>
  definitions.value.map(def => ({
    key: def.key,
    label: def.label,
    type: def.type === 'month' ? 'text' : def.type,
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

watch(
  () => props.item,
  item => {
    Object.keys(formModel).forEach(key => delete formModel[key])

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
  },
  { immediate: true },
)

const execute = () => {
  if (!props.item) return

  if (props.item.confirmMessage) {
    const ok = window.confirm(props.item.confirmMessage)
    if (!ok) return
  }

  emit('execute', {
    jobCode: props.item.jobCode,
    params: { ...formModel },
  })
}
</script>

<template>
  <v-dialog v-model="visible" max-width="760" persistent>
    <v-card>
      <v-card-title class="dialog-title">
        {{ item?.label ?? 'バッチ実行' }}
      </v-card-title>

      <v-divider />

      <v-card-text class="dialog-body">
        <div v-if="item" class="summary">
          <div><strong>jobCode：</strong>{{ item.jobCode }}</div>
        </div>

        <FormLayout v-model="formModel" :schema="schema">
          <GridBasedForm
            v-if="fields.length > 0"
            v-model="formModel"
            :fields="fields"
          />

          <div v-else class="empty-params">
            入力パラメータはありません。
          </div>
        </FormLayout>
      </v-card-text>

      <v-divider />

      <v-card-actions class="dialog-actions">
        <v-spacer />
        <v-btn
          color="secondary"
          variant="tonal"
          :disabled="loading"
          @click="visible = false"
        >
          閉じる
        </v-btn>

        <v-btn
          color="primary"
          :loading="loading"
          :disabled="!item || loading"
          @click="execute"
        >
          実行
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.dialog-title {
  font-weight: 700;
}

.dialog-body {
  display: grid;
  gap: 16px;
}

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

.dialog-actions {
  padding: 12px 16px;
}
</style>