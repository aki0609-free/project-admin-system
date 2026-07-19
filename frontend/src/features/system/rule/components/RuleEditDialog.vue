<script setup lang="ts">
import { computed, ref, toRef, watch } from 'vue'

import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import DslEditor from '@/shared/components/editor/DslEditor.vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import DetailDialogLayout from '@/toolbox/dialog/DetailDialogLayout.vue'

import type { RuleMasterResponse } from '@/features/system/rule/types/ruleApiTypes'
import type { RuleMasterForm } from '@/features/system/rule/types/ruleFormTypes'
import {
  createEmptyRuleMasterForm,
  toRuleMasterForm,
} from '@/features/system/rule/mapper/ruleFormMapper'

import RuleParameterTab from '@/features/system/rule/components/RuleParameterTab.vue'
import RuleDataSourceTab from '@/features/system/rule/components/RuleDataSourceTab.vue'
import RuleTestTab from '@/features/system/rule/components/RuleTestTab.vue'

const props = defineProps<{
  modelValue: boolean
  rule: RuleMasterResponse | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  save: [form: RuleMasterForm]
  delete: [form: RuleMasterForm]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const activeTab = ref<'basic' | 'dsl' | 'parameters' | 'datasources'>('basic')
const form = ref<RuleMasterForm>(createEmptyRuleMasterForm())

const isEdit = computed(() => form.value.id > 0)

const tabs = [
  { label: 'Basic', value: 'basic' },
  { label: 'DSL', value: 'dsl' },
  { label: 'Parameter', value: 'parameters' },
  { label: 'DataSource', value: 'datasources' },
]

const ruleTypeItems = [
  { title: 'GENERAL', value: 'GENERAL' },
  { title: 'ALLOWANCE', value: 'ALLOWANCE' },
  { title: 'DEDUCTION', value: 'DEDUCTION' },
  { title: 'DAILY_REPORT', value: 'DAILY_REPORT' },
  { title: 'MONTHLY_DETAIL', value: 'MONTHLY_DETAIL' },
  { title: 'PAYROLL', value: 'PAYROLL' },
]

const dslTypeItems = [
  { title: 'JEXL', value: 'JEXL' },
  { title: 'MVEL', value: 'MVEL' },
  { title: 'JAVA_BEAN', value: 'JAVA_BEAN' },
]

watch(
  () => props.modelValue,
  opened => {
    if (!opened) return

    form.value = props.rule
      ? toRuleMasterForm(props.rule)
      : createEmptyRuleMasterForm()

    activeTab.value = 'basic'
  },
)

watch(
  toRef(props, 'rule'),
  rule => {
    if (!visible.value) return

    form.value = rule
      ? toRuleMasterForm(rule)
      : createEmptyRuleMasterForm()
  },
)

const footerItems = computed<ToolbarItem[]>(() => {
  const items: ToolbarItem[] = [
    {
      type: 'button',
      label: 'キャンセル',
      onClick: () => {
        visible.value = false
      },
    },
    {
      type: 'button',
      label: isEdit.value ? '更新' : '作成',
      color: 'primary',
      onClick: () => emit('save', form.value),
    },
  ]

  if (isEdit.value) {
    items.unshift({
      type: 'button',
      label: '削除',
      color: 'error',
      onClick: () => emit('delete', form.value),
    })
  }

  return items
})
</script>

<template>
  <DetailDialogLayout
    v-model="visible"
    :title="isEdit ? 'Rule編集' : 'Rule新規作成'"
    max-width="1800"
    :footer-items="footerItems"
  >
    <div class="rule-editor-layout">
      <aside class="rule-side">
        <v-text-field
          v-model="form.ruleName"
          label="ruleName"
          variant="outlined"
          density="comfortable"
        />

        <v-text-field
          v-model="form.ruleDisplayName"
          label="表示名"
          variant="outlined"
          density="comfortable"
        />

        <v-select
          v-model="form.ruleType"
          label="RuleType"
          :items="ruleTypeItems"
          variant="outlined"
          density="comfortable"
        />

        <v-select
          v-model="form.dslType"
          label="DslType"
          :items="dslTypeItems"
          variant="outlined"
          density="comfortable"
        />

        <v-text-field
          v-model="form.resultFactKey"
          label="resultFactKey"
          variant="outlined"
          density="comfortable"
        />

        <v-text-field
          v-model.number="form.priority"
          label="priority"
          type="number"
          variant="outlined"
          density="comfortable"
        />

        <v-checkbox
          v-model="form.activeFlag"
          label="有効"
          density="compact"
          hide-details
        />
      </aside>

      <main class="rule-main">
        <TabLayout
          v-model="activeTab"
          :tabs="tabs"
        >
          <template #default="{ active }">
            <div
              v-if="active === 'basic'"
              class="tab-panel"
            >
              <v-textarea
                v-model="form.description"
                label="説明"
                variant="outlined"
                rows="6"
                auto-grow
              />

              <v-alert
                type="info"
                variant="tonal"
                density="compact"
              >
                Ruleは reduction / allowance / daily report などから ruleName で呼び出されます。
              </v-alert>

              <v-text-field
                v-if="form.dslType === 'JAVA_BEAN'"
                v-model="form.ruleBeanName"
                label="ruleBeanName"
                variant="outlined"
                density="comfortable"
              />
            </div>

            <div
              v-else-if="active === 'dsl'"
              class="tab-panel"
            >
              <DslEditor
                v-model="form.dslText"
                label="DSL"
                hint="JEXL例: employee.hourlyWage * params.hours"
                :rows="18"
              />
            </div>

            <div
              v-else-if="active === 'parameters'"
              class="tab-panel"
            >
              <RuleParameterTab :form="form" />
            </div>

            <div
              v-else-if="active === 'datasources'"
              class="tab-panel"
            >
              <RuleDataSourceTab :form="form" />
            </div>

            <div
              v-else-if="active === 'test'"
              class="tab-panel"
            >
              <RuleTestTab :form="form" />
            </div>
          </template>
        </TabLayout>
      </main>
    </div>
  </DetailDialogLayout>
</template>

<style scoped>
.rule-editor-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 20px;
  min-height: 720px;
}

.rule-side {
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #f8fafc;
}

.rule-main {
  min-width: 0;
}

.tab-panel {
  padding-top: 16px;
  display: grid;
  gap: 16px;
}

@media (max-width: 1100px) {
  .rule-editor-layout {
    grid-template-columns: 1fr;
  }
}
</style>