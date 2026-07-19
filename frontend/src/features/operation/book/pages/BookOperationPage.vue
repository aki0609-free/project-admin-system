<script setup lang="ts">
import { ref, computed } from 'vue'
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue'
import OperationReportTab from '@/features/operation/reportpreview/components/OperationReportTab.vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'

const targetMonth = ref(new Date().toISOString().slice(0, 7))

const leftToolbarItems = computed<ToolbarItem[]>(() => [])
const rightToolbarItems = computed<ToolbarItem[]>(() => [])
</script>

<template>
  <ListDetailPageLayout
    title="台帳管理"
    description="Excel台帳・月次台帳を確認、更新します。"
    :left-toolbar-items="leftToolbarItems"
    :right-toolbar-items="rightToolbarItems"
  >
    <template #search>
      <div class="book-search">
        <v-text-field
          v-model="targetMonth"
          type="month"
          label="対象月"
          variant="outlined"
          density="compact"
          hide-details
          prepend-inner-icon="mdi-calendar-month"
        />
      </div>
    </template>

    <OperationReportTab
      operation-type="BOOK"
      :target-month="targetMonth"
    />
  </ListDetailPageLayout>
</template>

<style scoped>
.book-search {
  max-width: 220px;
}
</style>