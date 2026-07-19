<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { z } from 'zod'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import TabbedForm from '@/shared/components/form/tabbed_form/TabbedForm.vue'
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

const form = reactive<ApplicantPersistedRow>(createEmptyApplicant(-1))
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
  <v-dialog v-model="dialogModel" max-width="1200">
    <v-card>
      <v-card-title>
        {{ isCreateMode ? '応募者 新規登録' : '応募者 編集' }}
      </v-card-title>

      <v-card-text>
        <FormLayout v-model="form" :schema="schema">
          <TabbedForm v-model="form" :tabs="[...tabs]" :fields="fields" />
        </FormLayout>
      </v-card-text>

      <v-card-actions>
        <v-btn v-if="!isCreateMode" color="error" variant="text" @click="handleDelete">
          削除
        </v-btn>
        <v-spacer />
        <v-btn variant="text" @click="handleClose">キャンセル</v-btn>
        <v-btn color="primary" @click="handleSave">保存</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
