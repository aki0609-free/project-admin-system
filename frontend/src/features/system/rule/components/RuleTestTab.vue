<script setup lang="ts">
import { computed, ref } from 'vue'

import DslEditor from '@/shared/components/editor/DslEditor.vue'
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useExecuteRuleMutation } from '@/features/system/rule/api/useExecuteRuleMutation'
import type { RuleMasterForm } from '@/features/system/rule/types/ruleFormTypes'
import type { RuleExecutionResponse } from '@/features/system/rule/types/ruleApiTypes'

const props = defineProps<{
  form: RuleMasterForm
}>()

const executeMutation = useExecuteRuleMutation()

const inputJson = ref(`{
  "employeeId": 1,
  "targetDate": "2026-07-01",
  "hours": 8
}`)

const result = ref<RuleExecutionResponse | null>(null)
const errorMessage = ref('')

const canExecute = computed(() =>
  props.form.ruleName.trim().length > 0,
)

const prettyResult = computed(() => {
  if (!result.value) return ''

  return JSON.stringify(
    result.value,
    null,
    2,
  )
})

const parseInput = () => {
  try {
    errorMessage.value = ''

    const parsed = JSON.parse(inputJson.value)

    if (
      parsed === null ||
      Array.isArray(parsed) ||
      typeof parsed !== 'object'
    ) {
      throw new Error('JSONはオブジェクト形式で入力してください。')
    }

    return parsed as Record<string, unknown>
  } catch (error) {
    errorMessage.value =
      error instanceof Error
        ? error.message
        : 'JSONの解析に失敗しました。'

    return null
  }
}

const execute = async () => {
  const facts = parseInput()

  if (!facts) return

  result.value = null

  const response = await executeMutation.mutateAsync({
    ruleName: props.form.ruleName,
    facts,
  })

  result.value = response
}

const clear = () => {
  result.value = null
  errorMessage.value = ''
}

const toolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '実行',
    color: 'primary',
    icon: 'mdi-play',
    disabled: !canExecute.value || executeMutation.isPending.value,
    onClick: execute,
  },
  {
    type: 'button',
    label: 'クリア',
    variant: 'text',
    icon: 'mdi-close',
    onClick: clear,
  },
])
</script>

<template>
  <div class="rule-test-tab">
    <GenericToolbar :items="toolbarItems" />

    <v-alert
      v-if="!canExecute"
      type="warning"
      variant="tonal"
      density="compact"
    >
      実行テストには ruleName が必要です。保存済みRuleでの実行を推奨します。
    </v-alert>

    <v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      density="compact"
    >
      {{ errorMessage }}
    </v-alert>

    <div class="test-layout">
      <div class="test-pane">
        <DslEditor
          v-model="inputJson"
          label="Input Facts(JSON)"
          hint="RuleExecutionServiceへ渡すfactsをJSONで入力します。"
          :rows="18"
        />
      </div>

      <div class="test-pane">
        <DslEditor
          :model-value="prettyResult"
          label="Execution Result"
          hint="実行結果とFact内容を表示します。"
          :rows="18"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.rule-test-tab {
  display: grid;
  gap: 12px;
}

.test-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
}

.test-pane {
  min-width: 0;
}

@media (max-width: 1100px) {
  .test-layout {
    grid-template-columns: 1fr;
  }
}
</style>