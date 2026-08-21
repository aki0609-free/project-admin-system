<script setup lang="ts">
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'

import NoticeRuleTable from '@/features/system/notice/components/NoticeRuleTable.vue'
import NoticeRuleEditDialog from '@/features/system/notice/components/NoticeRuleEditDialog.vue'
import { useNoticeRulePage } from '@/features/system/notice/composables/useNoticeRulePage'

const {
  rulesQuery,
  dialogVisible,
  dialogRule,
  leftToolbarItems,
  rightToolbarItems,
  openEdit,
  save,
  remove,
} = useNoticeRulePage()
</script>

<template>
  <ListDetailPageLayout
    title="お知らせルール管理"
    description="指定テーブルの日付カラムを定期チェックし、自動でお知らせを生成するルールを管理します。"
    :left-toolbar-items="leftToolbarItems"
    :right-toolbar-items="rightToolbarItems"
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
