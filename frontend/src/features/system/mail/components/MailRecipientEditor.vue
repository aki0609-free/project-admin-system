<script setup lang="ts">
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { MailRecipientForm } from '@/features/system/mail/types/mailFormTypes'
import MailRecipientTable from '@/features/system/mail/components/MailRecipientTable.vue'
import MailRecipientDetailPanel from '@/features/system/mail/components/MailRecipientDetailPanel.vue'
import type { ZodObject } from 'zod'

defineProps<{
  recipients: MailRecipientForm[]
  selectedRecipient: MailRecipientForm | null
  toolbarItems: ToolbarItem[]
  schema: ZodObject
  fields: GridFormFieldDef<MailRecipientForm>[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: MailRecipientForm): void
  (e: 'update:selectedRecipient', value: MailRecipientForm | null): void
}>()
</script>

<template>
  <div class="mail-recipient-editor">
    <GenericToolbar :items="toolbarItems" />

    <div class="recipient-pane">
      <div class="recipient-pane-left">
        <MailRecipientTable
          :items="recipients"
          @row-click="row => emit('row-click', row)"
        />
      </div>

      <div class="recipient-pane-right">
        <MailRecipientDetailPanel
          :model-value="selectedRecipient"
          :schema="schema"
          :fields="fields"
          @update:model-value="value => emit('update:selectedRecipient', value)"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.mail-recipient-editor {
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