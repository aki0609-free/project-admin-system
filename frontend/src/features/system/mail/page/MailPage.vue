<script setup lang="ts">
import { ref } from 'vue'

import MailQueueTab from '@/features/system/mail/components/MailQueueTab.vue'
import MailRecipientGroupTab from '@/features/system/mail/components/MailRecipientGroupTab.vue'
import MailTemplateTab from '@/features/system/mail/components/MailTemplateTab.vue'
import MailTestSendTab from '@/features/system/mail/components/MailTestSendTab.vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue'

const activeTab = ref<'recipientGroups' | 'templates' | 'queues' | 'testSend'>('recipientGroups')

const tabs = [
  { label: '宛先グループ', value: 'recipientGroups' },
  { label: 'メッセージテンプレート', value: 'templates' },
  { label: '送信キュー', value: 'queues' },
  { label: 'テスト送信', value: 'testSend' },
]
</script>

<template>
  <ListDetailPageLayout
    title="メール管理"
    description="宛先グループ、メッセージテンプレート、送信キュー、テストメール送信を管理します。"
  >
    <TabLayout v-model="activeTab" :tabs="tabs">
      <template #default="{ active }">
        <MailRecipientGroupTab v-if="active === 'recipientGroups'" />

        <MailTemplateTab v-else-if="active === 'templates'" />

        <MailQueueTab v-else-if="active === 'queues'" />

        <MailTestSendTab v-else-if="active === 'testSend'" />
      </template>
    </TabLayout>
  </ListDetailPageLayout>
</template>
