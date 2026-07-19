<script setup lang="ts">
import { computed, ref, toRef } from 'vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import DetailDialogLayout from '@/toolbox/dialog/DetailDialogLayout.vue'
import type { MailRecipientGroupResponse } from '@/features/system/mail/types/mailApiTypes'
import type {
  MailRecipientForm,
  MailRecipientGroupForm,
} from '@/features/system/mail/types/mailFormTypes'
import { useMailRecipientGroupEditDialog } from '@/features/system/mail/composables/useMailRecipientGroupEditDialog'
import MailRecipientEditor from './MailRecipientEditor.vue'

const props = defineProps<{
  modelValue: boolean
  group: MailRecipientGroupResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save' | 'delete', value: MailRecipientGroupForm): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const activeTab = ref<'basic' | 'recipients'>('basic')

const tabs = [
  { label: '基本情報', value: 'basic' },
  { label: '宛先', value: 'recipients' },
]

const {
  formModel,
  selectedRecipient,
  isEdit,
  fields,
  recipientFields,
  schema,
  recipientSchema,
  footerItems,
  recipientToolbarItems,
  selectRecipient,
} = useMailRecipientGroupEditDialog(
  visible,
  toRef(props, 'group'),
  form => emit('save', form),
  form => emit('delete', form),
)

const onRecipientRowClick = (row: MailRecipientForm) => {
  selectRecipient(row.id)
}
</script>

<template>
  <DetailDialogLayout
    v-model="visible"
    :title="isEdit ? '宛先グループ編集' : '宛先グループ新規作成'"
    max-width="1280"
    :footer-items="footerItems"
  >
    <TabLayout v-model="activeTab" :tabs="tabs">
      <template #default="{ active }">
        <div v-if="active === 'basic'" class="tab-page">
          <FormLayout v-model="formModel" :schema="schema">
            <GridBasedForm
              v-model="formModel"
              :fields="fields"
            />
          </FormLayout>
        </div>

        <div v-else-if="active === 'recipients'" class="tab-page">
          <MailRecipientEditor
            v-model:selected-recipient="selectedRecipient"
            :recipients="formModel.recipients"
            :toolbar-items="recipientToolbarItems"
            :schema="recipientSchema"
            :fields="recipientFields"
            @row-click="onRecipientRowClick"
          />
        </div>
      </template>
    </TabLayout>
  </DetailDialogLayout>
</template>

<style scoped>
.tab-page {
  display: grid;
  gap: 12px;
}

.recipient-pane {
  display: grid;
  grid-template-columns: minmax(520px, 1.1fr) minmax(420px, 1fr);
  gap: 16px;
  min-height: 420px;
}

.recipient-pane-left,
.recipient-pane-right {
  min-width: 0;
}

@media (max-width: 1100px) {
  .recipient-pane {
    grid-template-columns: 1fr;
  }
}
</style>