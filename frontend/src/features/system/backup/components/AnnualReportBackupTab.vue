<script setup lang="ts">
import { z } from 'zod'

import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import { useAnnualReportBackup } from '../composables/useAnnualReportBackup'
import type { AnnualReportBackupSetting } from '../types/annualReportBackupTypes'

const {
  loading,
  errorMessage,
  successMessage,
  setting,
  manualFiscalYear,
  lastResult,
  save,
  execute,
} = useAnnualReportBackup()

const schema = z.object({
  fiscalYearStartMonth: z.number().min(1).max(12),
  graceDays: z.number().min(0).max(90),
  startupEnabled: z.boolean(),
  activeFlag: z.boolean(),
})

const fields: GridFormFieldDef<AnnualReportBackupSetting>[] = [
  {
    key: 'fiscalYearStartMonth',
    label: '会計年度の開始月',
    type: 'select',
    options: Array.from({ length: 12 }, (_, index) => ({
      title: `${index + 1}月`,
      value: index + 1,
    })),
  },
  { key: 'graceDays', label: '年度終了後の猶予日数', type: 'number' },
  { key: 'startupEnabled', label: '起動時に未処理年度を自動確認', type: 'checkbox' },
  { key: 'activeFlag', label: '設定を有効にする', type: 'checkbox' },
]
</script>

<template>
  <section class="annual-report-backup-tab">
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

    <div class="setting-block">
      <div>
        <h2>年度帳票バックアップ</h2>
        <p>
          年度終了後、設定した猶予日数を過ぎてから最初にシステムが起動した時点で、
          保存対象の月次帳票を年度バックアップへコピーします。
        </p>
      </div>
      <v-alert type="info" variant="tonal">
        指定日時に24時間稼働させる必要はありません。停止中だった場合は、次回起動時に未処理年度を確認します。
      </v-alert>
      <FormLayout v-model="setting" :schema="schema">
        <GridBasedForm v-model="setting" :fields="fields" />
      </FormLayout>
      <div class="actions">
        <v-btn color="primary" :loading="loading" @click="save">
          バックアップ設定を保存
        </v-btn>
      </div>
    </div>

    <v-divider />

    <div class="setting-block">
      <div>
        <h2>手動実行</h2>
        <p>
          障害時の再確認や初回移行時に、対象年度を指定して実行できます。完了済み年度は重複作成しません。
        </p>
      </div>
      <div class="manual-backup-row">
        <v-text-field
          v-model.number="manualFiscalYear"
          label="対象年度"
          type="number"
          min="2000"
          max="2200"
          suffix="年度"
          variant="outlined"
          hide-details
        />
        <v-btn color="primary" variant="tonal" :loading="loading" @click="execute">
          今すぐ実行
        </v-btn>
      </div>
      <v-alert
        v-if="lastResult"
        :type="lastResult.status === 'COMPLETED' ? 'success' : 'error'"
        variant="tonal"
      >
        {{ lastResult.fiscalYear }}年度: {{ lastResult.status }} ／
        {{ lastResult.fileCount }}ファイル ／
        {{ lastResult.totalSize.toLocaleString() }} bytes
        <span v-if="lastResult.errorMessage">（{{ lastResult.errorMessage }}）</span>
      </v-alert>
    </div>
  </section>
</template>

<style scoped>
.annual-report-backup-tab {
  display: grid;
  gap: 24px;
  max-width: 800px;
  padding-top: 16px;
}

.setting-block {
  display: grid;
  gap: 16px;
}

h2 {
  margin: 0;
}

p {
  margin: 6px 0 0;
  color: #475569;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.manual-backup-row {
  display: grid;
  grid-template-columns: minmax(220px, 320px) auto;
  align-items: center;
  gap: 12px;
}

@media (max-width: 600px) {
  .manual-backup-row {
    grid-template-columns: 1fr;
  }
}
</style>
