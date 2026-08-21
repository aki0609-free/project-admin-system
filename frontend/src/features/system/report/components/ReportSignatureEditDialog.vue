<script setup lang="ts">
import { computed, toRef } from 'vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import { useReportSignatureDetailQuery } from '@/features/system/report/api/queries/useReportSignatureDetailQuery'
import { useReportSignatureEditDialog } from '@/features/system/report/composables/useReportSignatureEditDialog'
import ReportSignatureImageField from '@/features/system/report/components/ReportSignatureImageField.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'

const props = defineProps<{
  modelValue: boolean
  reportSignatureId: number | null
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

const detailQuery = useReportSignatureDetailQuery(
  computed(() => props.reportSignatureId),
)

const {
  formModel,
  previewSrc,
  isEdit,
  fields,
  schema,
  leftFooterItems,
  rightFooterItems,
  onFileChange,
} = useReportSignatureEditDialog(
  visible,
  toRef(props, 'reportSignatureId'),
  detailQuery.reportSignature,
  close,
)
</script>

<template>
  <AppDialog
    v-model="visible"
    :title="isEdit ? 'Signature編集' : 'Signature新規作成'"
    size="lg"
    :max-width="960"
    body-layout="stack"
    :left-footer-items="leftFooterItems"
    :right-footer-items="rightFooterItems"
  >
    <FormLayout
      v-model="formModel"
      :schema="schema"
    >
      <GridBasedForm
        v-model="formModel"
        :fields="fields"
      />
    </FormLayout>

    <ReportSignatureImageField
      :preview-src="previewSrc"
      @file-change="onFileChange"
    />
  </AppDialog>
</template>
