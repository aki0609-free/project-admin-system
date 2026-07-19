<script setup lang="ts">
import { computed } from 'vue'
import type { ZodObject } from 'zod'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import DetailPanelLayout from '@/toolbox/panel/DetailPanelLayout.vue'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { MailRecipientForm } from '@/features/system/mail/types/mailFormTypes'

const props = defineProps<{
  modelValue: MailRecipientForm | null
  schema: ZodObject
  fields: GridFormFieldDef<MailRecipientForm>[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: MailRecipientForm | null): void
}>()

const recipient = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})
</script>

<template>
  <DetailPanelLayout
    :has-data="!!recipient"
    title="選択中宛先"
    subtitle="RECIPIENT DETAIL"
    :chip="recipient?.recipientType ?? null"
    empty-message="左の一覧から宛先を選択してください。新規追加する場合は「追加」を押してください。"
  >
    <FormLayout
      v-if="recipient"
      v-model="recipient"
      :schema="schema"
    >
      <GridBasedForm
        v-model="recipient"
        :fields="fields"
      />
    </FormLayout>
  </DetailPanelLayout>
</template>