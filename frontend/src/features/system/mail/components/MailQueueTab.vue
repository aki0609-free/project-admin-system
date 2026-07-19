<script setup lang="ts">
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import { useMailQueueTab } from '@/features/system/mail/composables/useMailQueueTab'

const { rows, columns, filterRules, toolbarItems, retryMutation, retry } = useMailQueueTab()
</script>

<template>
  <div class="mail-tab">
    <div class="tab-header">
      <h3>送信キュー</h3>
      <p class="tab-description">メール送信状態の確認、WAITING送信、FAILED再送を行います。</p>
    </div>

    <GenericToolbar :items="toolbarItems" />

    <SimpleTable
      table-key="mail-send-queue-list"
      item-key="id"
      :items="rows"
      :columns="columns"
      :filter-rules="filterRules"
    >
      <template #[`item.status`]="{ item }">
        <v-chip
          size="small"
          :color="
            item.status === 'SENT' ? 'success' : item.status === 'FAILED' ? 'error' : 'primary'
          "
          variant="tonal"
        >
          {{ item.status }}
        </v-chip>
      </template>

      <template #[`item.errorMessage`]="{ item }">
        <div class="error-cell">
          <span>{{ item.errorMessage }}</span>

          <v-btn
            v-if="item.status === 'FAILED'"
            size="x-small"
            color="primary"
            variant="outlined"
            :loading="retryMutation.isPending.value"
            @click.stop="retry(item)"
          >
            retry
          </v-btn>
        </div>
      </template>
    </SimpleTable>
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

.error-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
