<script setup lang="ts">
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import MailTemplateTable from '@/features/system/mail/components/MailTemplateTable.vue'
import MailTemplateEditDialog from '@/features/system/mail/components/MailTemplateEditDialog.vue'
import { useMailTemplateTab } from '@/features/system/mail/composables/useMailTemplateTab'

const {
  templatesQuery,
  visible,
  dialogTemplate,
  toolbarItems,
  openEdit,
  save,
  remove,
} = useMailTemplateTab()
</script>

<template>
  <div class="mail-tab">
    <div class="tab-header">
      <h3>メッセージテンプレート</h3>
      <p class="tab-description">
        通知メールや帳票メールで使用する件名・本文を管理します。
      </p>
    </div>

    <GenericToolbar :items="toolbarItems" />

    <MailTemplateTable
      :items="templatesQuery.templates.value"
      @row-click="openEdit"
    />

    <MailTemplateEditDialog
      v-model="visible"
      :template="dialogTemplate"
      @save="save"
      @delete="remove"
    />
  </div>
</template>

<style scoped>
.mail-tab {
  display: grid;
  gap: 12px;
}

.tab-header {
  display: grid;
  gap: 6px;
}

.tab-description {
  margin: 0;
  color: #64748b;
}
</style>
