<script setup lang="ts">
import { computed, ref } from 'vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import { useReportOperationSettings } from '../composables/useReportOperationSettings'

const tabs = [
  { label: '締め帳票', value: 'closingOutputs' },
  { label: 'プレビュー帳票', value: 'previewReports' },
]
const activeTab = ref('closingOutputs')

const {
  loading,
  errorMessage,
  successMessage,
  closingOutputs,
  previewReports,
  previewReportDialog,
  editingPreviewReport,
  previewTemplateFile,
  saveOutputs,
  openPreviewReportCreate,
  openPreviewReportEdit,
  savePreviewReport,
} = useReportOperationSettings()

const operationTypeOptions = [
  { title: '現場配置・配車', value: 'PREPARATION' },
  { title: '日次管理', value: 'DAILY' },
  { title: '月次管理', value: 'MONTHLY' },
  { title: '台帳', value: 'BOOK' },
]
const outputTypeOptions = [
  { title: '画面プレビュー', value: 'HTML_PREVIEW' },
  { title: 'ブラウザ印刷', value: 'HTML_PRINT' },
]
const operationTypeLabel = (value: string) =>
  operationTypeOptions.find(item => item.value === value)?.title ?? value
const outputTypeLabel = (value: string) =>
  outputTypeOptions.find(item => item.value === value)?.title ?? value
const updatePreviewTemplate = (value: File | File[] | null) => {
  previewTemplateFile.value = Array.isArray(value) ? (value[0] ?? null) : value
}
const footerItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '閉じる',
    intent: 'secondary',
    onClick: () => { previewReportDialog.value = false },
  },
  {
    type: 'button',
    label: '保存',
    intent: 'primary',
    loading: loading.value,
    onClick: savePreviewReport,
  },
])
</script>

<template>
  <div class="report-operation-settings">
    <v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      closable
      @click:close="errorMessage = ''"
    >
      {{ errorMessage }}
    </v-alert>
    <v-alert
      v-if="successMessage"
      type="success"
      variant="tonal"
      closable
      @click:close="successMessage = ''"
    >
      {{ successMessage }}
    </v-alert>

    <v-card :loading="loading" variant="outlined">
      <TabLayout v-model="activeTab" :tabs="tabs">
        <template #default="{ active }">
          <section v-if="active === 'closingOutputs'" class="settings-section">
            <div>
              <h2>月次締め帳票</h2>
              <p>月次締めで生成する帳票、実行順、保存年数を設定します。</p>
            </div>
            <v-table density="comfortable">
              <thead>
                <tr><th>生成</th><th>順序</th><th>帳票</th><th>形式</th><th>保存年数</th><th>必須</th></tr>
              </thead>
              <tbody>
                <tr v-for="item in closingOutputs" :key="item.reportCode">
                  <td><v-checkbox v-model="item.activeFlag" hide-details density="compact" /></td>
                  <td>
                    <v-text-field v-model.number="item.executionOrder" type="number" min="1" hide-details density="compact" />
                  </td>
                  <td>
                    <div class="item-name">{{ item.reportName || item.reportCode }}</div>
                    <div class="item-description">{{ item.reportCode }} / {{ item.jobCode }}</div>
                  </td>
                  <td>{{ item.outputType }}</td>
                  <td>
                    <v-text-field
                      v-model.number="item.backupRetentionYears"
                      type="number"
                      min="1"
                      max="7"
                      placeholder="未設定"
                      suffix="年"
                      hide-details
                      density="compact"
                    />
                  </td>
                  <td>{{ item.requiredFlag ? '必須' : '任意' }}</td>
                </tr>
              </tbody>
            </v-table>
            <div class="actions">
              <v-btn color="primary" :loading="loading" @click="saveOutputs">締め帳票設定を保存</v-btn>
            </div>
          </section>

          <section v-else-if="active === 'previewReports'" class="settings-section">
            <div class="section-heading">
              <div>
                <h2>プレビュー帳票</h2>
                <p>参照ViewとHTMLテンプレートを紐づけ、業務画面の簡易プレビューを管理します。</p>
              </div>
              <v-btn color="primary" variant="tonal" @click="openPreviewReportCreate">プレビュー帳票を追加</v-btn>
            </div>
            <v-alert type="info" variant="tonal">
              HTMLテンプレートはVersion 1へ保存します。既存定義でHTMLを選び直すと同じVersion 1を更新します。
            </v-alert>
            <v-table density="comfortable">
              <thead>
                <tr><th>処理</th><th>帳票</th><th>データ元View/Table</th><th>形式</th><th>テンプレート</th><th>状態</th><th /></tr>
              </thead>
              <tbody>
                <tr v-for="item in previewReports" :key="item.id ?? item.reportCode">
                  <td>{{ operationTypeLabel(item.operationType) }}</td>
                  <td>
                    <div class="item-name">{{ item.reportName }}</div>
                    <div class="item-description">{{ item.reportCode }}</div>
                  </td>
                  <td>
                    <div>{{ item.tableName }}</div>
                    <div class="item-description">絞込: {{ item.filterColumnName || '区分ごとの標準列' }}</div>
                  </td>
                  <td>{{ outputTypeLabel(item.outputType) }}</td>
                  <td>
                    <v-chip size="small" :color="item.templateExists ? 'success' : 'error'" variant="tonal">
                      {{ item.templateExists ? '登録済み' : '未登録' }} / v1
                    </v-chip>
                  </td>
                  <td>{{ item.activeFlag ? '有効' : '無効' }}</td>
                  <td class="row-actions">
                    <v-btn size="small" variant="text" @click="openPreviewReportEdit(item)">編集</v-btn>
                  </td>
                </tr>
                <tr v-if="previewReports.length === 0">
                  <td colspan="7" class="empty-row">プレビュー帳票は登録されていません。</td>
                </tr>
              </tbody>
            </v-table>
          </section>
        </template>
      </TabLayout>
    </v-card>

    <AppDialog
      v-model="previewReportDialog"
      :title="editingPreviewReport.id == null ? 'プレビュー帳票追加' : 'プレビュー帳票編集'"
      size="xl"
      body-layout="stack"
      :right-footer-items="footerItems"
    >
      <v-alert type="info" variant="tonal" class="mb-4">
        HTMLでは <code>rows</code>（Viewの行一覧）、<code>definition</code>、<code>request</code> を参照できます。
      </v-alert>
      <div class="preview-report-grid">
        <v-select v-model="editingPreviewReport.operationType" label="表示する処理" :items="operationTypeOptions" :disabled="editingPreviewReport.id != null" variant="outlined" />
        <v-select v-model="editingPreviewReport.outputType" label="表示形式" :items="outputTypeOptions" variant="outlined" />
        <v-text-field v-model="editingPreviewReport.reportCode" label="帳票コード" hint="半角英字で始まる英数字・アンダースコア" persistent-hint :disabled="editingPreviewReport.id != null" variant="outlined" />
        <v-text-field v-model="editingPreviewReport.reportName" label="帳票名" variant="outlined" />
        <v-text-field v-model="editingPreviewReport.tableName" label="データ元View/Table" hint="tenant_idと対象日または対象月の列が必要です" persistent-hint variant="outlined" />
        <v-text-field v-model="editingPreviewReport.filterColumnName" label="対象日・月の絞込列" placeholder="空欄なら処理区分の標準列" variant="outlined" />
        <v-text-field v-model="editingPreviewReport.orderBy" label="並び順" placeholder="例: payment_cycle_order, employee_code" variant="outlined" />
        <v-text-field v-model.number="editingPreviewReport.displayOrder" label="表示順" type="number" min="1" variant="outlined" />
        <v-file-input
          accept="text/html,.html"
          label="HTMLテンプレート"
          :hint="editingPreviewReport.id == null ? '新規登録時は必須です' : '変更するとVersion 1を上書きします'"
          persistent-hint
          prepend-icon="mdi-file-code-outline"
          variant="outlined"
          class="preview-template-input"
          @update:model-value="updatePreviewTemplate"
        />
        <div class="preview-report-flags">
          <v-checkbox v-model="editingPreviewReport.activeFlag" label="有効" hide-details />
          <v-chip size="small" variant="tonal">Template Version 1</v-chip>
        </div>
      </div>
      <v-text-field
        v-if="editingPreviewReport.htmlTemplateKey"
        :model-value="editingPreviewReport.htmlTemplateKey"
        label="保存先（自動生成）"
        readonly
        variant="outlined"
        hide-details
      />
    </AppDialog>
  </div>
</template>

<style scoped>
.report-operation-settings,
.settings-section {
  display: grid;
  gap: 16px;
}
.settings-section {
  padding: 24px;
  background: rgb(var(--v-theme-surface));
}
.settings-section h2 {
  margin: 0;
}
.settings-section p {
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
.actions {
  display: flex;
  justify-content: flex-end;
}
.item-name {
  font-weight: 700;
}
.item-description {
  margin-top: 2px;
  font-size: 12px;
  opacity: 0.72;
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
.preview-report-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.preview-template-input,
.preview-report-flags {
  grid-column: 1 / -1;
}
.preview-report-flags {
  display: flex;
  align-items: center;
  gap: 16px;
}
@media (max-width: 700px) {
  .preview-report-grid {
    grid-template-columns: 1fr;
  }
  .section-heading {
    flex-direction: column;
  }
}
</style>
