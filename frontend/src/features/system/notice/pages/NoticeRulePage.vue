<script setup lang="ts">
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue'

import NoticeRuleTable from '@/features/system/notice/components/NoticeRuleTable.vue'
import NoticeRuleEditDialog from '@/features/system/notice/components/NoticeRuleEditDialog.vue'
import { useNoticeRulePage } from '@/features/system/notice/composables/useNoticeRulePage'

const {
  rulesQuery,
  dialogVisible,
  dialogRule,
  toolbarItems,
  openEdit,
  save,
  remove,
} = useNoticeRulePage()
</script>

<template>
  <ListDetailPageLayout
    title="NoticeRule管理"
    description="指定テーブルの日付カラムを定期チェックし、自動でお知らせを生成するルールを管理します。"
    :right-toolbar-items="toolbarItems"
  >
    <NoticeRuleTable
      :items="rulesQuery.rules.value"
      @row-click="openEdit"
    />

    <template #dialogs>
      <NoticeRuleEditDialog
        v-model="dialogVisible"
        :rule="dialogRule"
        @save="save"
        @delete="remove"
      />
    </template>
  </ListDetailPageLayout>
</template>