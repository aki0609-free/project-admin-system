<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { z } from 'zod'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import TabbedForm from '@/shared/components/form/tabbed_form/TabbedForm.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import type { ApplicantPersistedRow } from '@/features/application/types/applicantTypes'
import { useApplicantFormFields } from '@/features/application/composables/applicant/useApplicantFormFields'
import { createEmptyApplicant } from '@/features/application/utils/createEmptyApplicantForm'

const props = defineProps<{
  modelValue: boolean
  applicant: ApplicantPersistedRow | null
  isCreateMode: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', value: ApplicantPersistedRow): void
  (e: 'delete', id: number): void
}>()

const dialogModel = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const { tabs, fields } = useApplicantFormFields()

const form = reactive<ApplicantPersistedRow>(createEmptyApplicant(1))
const formLayoutRef = ref<{ validateAll: () => boolean } | null>(null)

watch(
  () => props.applicant,
  (value) => {
    if (!value) return
    Object.assign(form, value)
  },
  { immediate: true },
)

const schema = z.object({
  name: z.string().min(1, '氏名は必須です'),
  furiganaName: z.string().optional(),
})

const leftFooterItems = computed<ToolbarItem[]>(() => [
  {
    id: 'delete',
    type: 'button',
    label: '削除',
    intent: 'danger',
    visible: !props.isCreateMode,
    onClick: handleDelete,
  },
])

const rightFooterItems = computed<ToolbarItem[]>(() => [
  {
    id: 'cancel',
    type: 'button',
    label: 'キャンセル',
    intent: 'secondary',
    onClick: handleClose,
  },
  {
    id: 'save',
    type: 'button',
    label: '保存',
    intent: 'primary',
    onClick: handleSave,
  },
])

function handleClose() {
  dialogModel.value = false
}

function handleSave() {
  const isValid = formLayoutRef.value?.validateAll() ?? false
  if (!isValid) return

  emit('save', { ...form })
}

function handleDelete() {
  if (!form.id) return

  if (!confirm(`応募者「${form.name}」を削除しますか？`)) return

  emit('delete', form.id)
  dialogModel.value = false
}
</script>

<template>
  <AppDialog
    v-model="dialogModel"
    :title="isCreateMode ? '応募者 新規登録' : '応募者 編集'"
    size="xl"
    :max-width="1200"
    :left-footer-items="leftFooterItems"
    :right-footer-items="rightFooterItems"
  >
    <FormLayout ref="formLayoutRef" v-model="form" :schema="schema">
      <TabbedForm v-model="form" :tabs="[...tabs]" :fields="fields" />
    </FormLayout>
  </AppDialog>
</template>
