<script setup lang="ts">
import AppToolbar from '@/shared/ui/toolbar/AppToolbar.vue'
import MailRecipientGroupTable from '@/features/system/mail/components/MailRecipientGroupTable.vue'
import MailRecipientGroupEditDialog from '@/features/system/mail/components/MailRecipientGroupEditDialog.vue'
import { useMailRecipientGroupTab } from '@/features/system/mail/composables/useMailRecipientGroupTab'

const {
  groupsQuery,
  visible,
  dialogGroup,
  leftToolbarItems,
  openEdit,
  save,
  remove,
} = useMailRecipientGroupTab()
</script>

<template>
  <div class="mail-tab">
    <div class="tab-header">
      <h3>宛先グループ</h3>
      <p class="tab-description">
        帳票メールや通知メールで使用する宛先グループを管理します。
      </p>
    </div>

    <AppToolbar :left-items="leftToolbarItems" surface="plain" />

    <MailRecipientGroupTable
      :items="groupsQuery.groups.value"
      @row-click="openEdit"
    />

    <MailRecipientGroupEditDialog
      v-model="visible"
      :group="dialogGroup"
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
