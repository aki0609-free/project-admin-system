<script setup lang="ts">
import MonthFormField from '@/shared/components/form/base/components/form/MonthFormField.vue'
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'
import { useYearEndAdjustmentPage } from '../composables/useYearEndAdjustmentPage'

const {
  loading,
  errorMessage,
  successMessage,
  taxYear,
  adjustments,
  employees,
  editing,
  loadAdjustments,
  editAdjustment,
  saveAdjustment,
  removeAdjustment,
} = useYearEndAdjustmentPage()
</script>

<template>
  <ListDetailPageLayout
    title="年末調整"
    description="従業員ごとの年末調整額と、月次給与へ反映する対象月を管理します。"
  >
    <v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      closable
      class="mb-4"
      @click:close="errorMessage = ''"
    >
      {{ errorMessage }}
    </v-alert>
    <v-alert
      v-if="successMessage"
      type="success"
      variant="tonal"
      closable
      class="mb-4"
      @click:close="successMessage = ''"
    >
      {{ successMessage }}
    </v-alert>

    <v-card :loading="loading" variant="outlined" class="year-end-card">
      <section class="year-end-section">
        <div class="section-heading">
          <div>
            <h2>年末調整一覧</h2>
            <p>正の金額は還付、負の金額は追加徴収です。確定した金額だけが指定月の月次給与明細に反映されます。</p>
          </div>
          <div class="inline-actions">
            <v-text-field
              v-model.number="taxYear"
              label="対象年度"
              type="number"
              min="2000"
              max="2200"
              hide-details
            />
            <v-btn variant="tonal" @click="loadAdjustments">表示</v-btn>
            <v-btn color="primary" variant="tonal" @click="editAdjustment()">新規追加</v-btn>
          </div>
        </div>

        <div class="year-end-editor">
          <v-select
            v-model="editing.employeeId"
            label="従業員"
            :items="employees"
            item-title="employeeName"
            item-value="id"
            variant="outlined"
            hide-details
          />
          <MonthFormField
            v-model="editing.targetMonth"
            label="反映対象月"
            variant="outlined"
            hide-details
          />
          <v-text-field
            v-model.number="editing.adjustmentAmount"
            label="年末調整額"
            type="number"
            suffix="円"
            variant="outlined"
            hide-details
          />
          <v-select
            v-model="editing.status"
            label="状態"
            :items="[
              { title: '下書き', value: 'DRAFT' },
              { title: '確定', value: 'CONFIRMED' },
            ]"
            variant="outlined"
            hide-details
          />
          <v-text-field v-model="editing.note" label="備考" variant="outlined" hide-details />
          <v-btn color="primary" :loading="loading" @click="saveAdjustment">保存</v-btn>
        </div>

        <v-table density="comfortable">
          <thead>
            <tr><th>社員コード</th><th>氏名</th><th>反映月</th><th>調整額</th><th>状態</th><th /></tr>
          </thead>
          <tbody>
            <tr v-for="item in adjustments" :key="item.id ?? `${item.employeeId}-${item.taxYear}`">
              <td>{{ item.employeeCode }}</td>
              <td>{{ item.employeeName }}</td>
              <td>{{ item.targetMonth }}</td>
              <td>{{ Number(item.adjustmentAmount).toLocaleString() }}円</td>
              <td>{{ item.status === 'CONFIRMED' ? '確定' : '下書き' }}</td>
              <td class="row-actions">
                <v-btn size="small" variant="text" @click="editAdjustment(item)">編集</v-btn>
                <v-btn size="small" color="error" variant="text" @click="removeAdjustment(item)">削除</v-btn>
              </td>
            </tr>
            <tr v-if="adjustments.length === 0">
              <td colspan="6" class="empty-row">対象年度の年末調整は登録されていません。</td>
            </tr>
          </tbody>
        </v-table>
      </section>
    </v-card>
  </ListDetailPageLayout>
</template>

<style scoped>
.year-end-card,
.year-end-section {
  background: rgb(var(--v-theme-surface));
}
.year-end-section {
  display: grid;
  gap: 16px;
  padding: 24px;
}
.year-end-section h2 {
  margin: 0;
}
.year-end-section p {
  margin: 6px 0 0;
  color: rgb(var(--v-theme-on-surface));
  opacity: 0.72;
}
.section-heading {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}
.inline-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.inline-actions .v-text-field {
  width: 150px;
}
.year-end-editor {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 8px;
}
.row-actions {
  white-space: nowrap;
  text-align: right;
}
.empty-row {
  padding: 32px !important;
  text-align: center;
  opacity: 0.72;
}
@media (max-width: 800px) {
  .section-heading,
  .inline-actions {
    align-items: stretch;
    flex-direction: column;
  }
  .inline-actions .v-text-field {
    width: auto;
  }
}
</style>
