<script setup lang="ts">
import { computed, toRef } from 'vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import ReportMasterBasicTab from '@/features/system/report/components/ReportMasterBasicTab.vue'
import ReportMasterExecutionTab from '@/features/system/report/components/ReportMasterExecutionTab.vue'
import ReportMasterParamsTab from '@/features/system/report/components/ReportMasterParamsTab.vue'
import { useReportMasterEditDialog } from '@/features/system/report/composables/useReportMasterEditDialog'
import DetailDialogLayout from '@/toolbox/dialog/DetailDialogLayout.vue'

const props = defineProps<{
  modelValue: boolean
  reportMasterId: number | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const close = () => {
  visible.value = false
}

const {
  activeTab,
  form,
  tabs,
  schema,
  templateOptions,
  isEdit,
  leftItems,
  rightItems,
} = useReportMasterEditDialog(
  visible,
  toRef(props, 'reportMasterId'),
  close,
)
</script>

<template>
  <DetailDialogLayout
    v-model="visible"
    :title="isEdit ? '帳票定義編集' : '帳票定義新規作成'"
    :max-width="1280"
    :left-footer-items="leftItems"
    :right-footer-items="rightItems"
  >
    <FormLayout
      v-model="form"
      :schema="schema"
    >
      <TabLayout
        v-model="activeTab"
        :tabs="tabs"
      >
        <template #default="{ active }">
          <ReportMasterBasicTab
            v-if="active === 'basic'"
            v-model="form"
            :template-options="templateOptions"
          />

          <ReportMasterExecutionTab
            v-else-if="active === 'execution'"
            v-model="form"
          />

          <ReportMasterParamsTab
            v-else-if="active === 'params'"
            v-model="form"
          />
        </template>
      </TabLayout>
    </FormLayout>
  </DetailDialogLayout>
</template>