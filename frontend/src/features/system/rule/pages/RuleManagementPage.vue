<script setup lang="ts">
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue'
import RuleTable from '@/features/system/rule/components/RuleTable.vue'
import RuleEditDialog from '@/features/system/rule/components/RuleEditDialog.vue'
import { useRulePage } from '@/features/system/rule/composables/useRulePage'

const {
  rulesQuery,
  dialogVisible,
  dialogRule,
  leftToolbarItems,
  openEdit,
  save,
  remove,
} = useRulePage()
</script>

<template>
  <ListDetailPageLayout
    title="Rule管理"
    description="控除・手当・日報・月次明細などで使用する計算Ruleを管理します。"
    :left-toolbar-items="leftToolbarItems"
  >
    <RuleTable
      :items="rulesQuery.rules.value"
      @row-click="openEdit"
    />

    <template #dialogs>
      <RuleEditDialog
        v-model="dialogVisible"
        :rule="dialogRule"
        @save="save"
        @delete="remove"
      />
    </template>
  </ListDetailPageLayout>
</template>