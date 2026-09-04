<script setup lang="ts">
import { computed } from 'vue'
import { z } from 'zod'
import DayRuleField from '@/shared/components/form/base/components/form/DayRuleField.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import { useBusinessSettingsPage } from '../composables/useBusinessSettingsPage'
import type {
  AnnualReportBackupSetting,
  ExternalSupportLinkSetting,
  ResignationChecklistItem,
  ResignationMessage,
} from '../types/businessSettingTypes'

const {
  activeTab,
  loading,
  errorMessage,
  successMessage,
  message,
  checklist,
  closingSetting,
  closingOutputs,
  annualReportBackup,
  externalSupportLinks,
  manualBackupFiscalYear,
  lastBackupResult,
  checklistDialog,
  editingChecklist,
  saveMessage,
  openChecklistCreate,
  openChecklistEdit,
  saveChecklist,
  removeChecklist,
  saveClosing,
  saveOutputs,
  saveBackupSetting,
  executeBackup,
  saveExternalSupportLinks,
} = useBusinessSettingsPage()

const resignationMessageSchema = z.object({
  dialogTitle: z.string().min(1, '必須です'),
  guidanceMessage: z.string().min(1, '必須です'),
  confirmationMessage: z.string().min(1, '必須です'),
})

const resignationMessageFields: GridFormFieldDef<ResignationMessage>[] = [
  { key: 'dialogTitle', label: 'ダイアログタイトル', type: 'text', gridColumn: '1 / -1' },
  {
    key: 'guidanceMessage',
    label: '案内文',
    type: 'textarea',
    rows: 4,
    autoGrow: true,
    gridColumn: '1 / -1',
  },
  {
    key: 'confirmationMessage',
    label: '警告見出し',
    type: 'textarea',
    rows: 2,
    autoGrow: true,
    gridColumn: '1 / -1',
  },
]

const backupSettingSchema = z.object({
  fiscalYearStartMonth: z.number().min(1).max(12),
  graceDays: z.number().min(0).max(90),
  startupEnabled: z.boolean(),
  activeFlag: z.boolean(),
})

const backupSettingFields: GridFormFieldDef<AnnualReportBackupSetting>[] = [
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

const supportLinkSchema = z.object({
  incidentReportUrl: z
    .string()
    .url('URL形式で入力してください')
    .startsWith('https://', 'HTTPSのURLを入力してください'),
  manualUrl: z
    .string()
    .url('URL形式で入力してください')
    .startsWith('https://', 'HTTPSのURLを入力してください'),
})

const supportLinkFields: GridFormFieldDef<ExternalSupportLinkSetting>[] = [
  {
    key: 'incidentReportUrl',
    label: 'インシデント報告のURL',
    type: 'text',
    gridColumn: '1 / -1',
  },
  {
    key: 'manualUrl',
    label: 'マニュアルのURL',
    type: 'text',
    gridColumn: '1 / -1',
  },
]

const checklistSchema = z.object({
  id: z.number(),
  code: z.string().min(1, '必須です'),
  name: z.string().min(1, '必須です'),
  description: z.string().nullable(),
  requiredFlag: z.boolean(),
  displayOrder: z.number().min(0),
  activeFlag: z.boolean(),
})

const checklistFields = computed<GridFormFieldDef<ResignationChecklistItem>[]>(() => [
  {
    key: 'code',
    label: 'TODOコード',
    type: 'text',
    editable: editingChecklist.id <= 0,
    gridColumn: '1 / span 2',
  },
  { key: 'name', label: '項目名', type: 'text', gridColumn: '3 / span 2' },
  {
    key: 'description',
    label: '説明',
    type: 'textarea',
    rows: 3,
    autoGrow: true,
    gridColumn: '1 / -1',
  },
  { key: 'displayOrder', label: '表示順', type: 'number' },
  { key: 'requiredFlag', label: '必須項目', type: 'checkbox' },
  { key: 'activeFlag', label: '有効', type: 'checkbox' },
])

const checklistFooterItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '閉じる',
    intent: 'secondary',
    onClick: () => {
      checklistDialog.value = false
    },
  },
  {
    type: 'button',
    label: '保存',
    intent: 'primary',
    loading: loading.value,
    onClick: saveChecklist,
  },
])
</script>

<template>
  <ListDetailPageLayout
    title="業務管理"
    description="退職処理、給与締日、月次締め帳票、年度バックアップ、外部リンクを管理します。"
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

    <v-card :loading="loading" variant="outlined">
      <v-tabs v-model="activeTab" color="primary">
        <v-tab value="resignation">退職時設定</v-tab>
        <v-tab value="closing">締日設定</v-tab>
        <v-tab value="outputs">締め帳票</v-tab>
        <v-tab value="backup">帳票バックアップ</v-tab>
        <v-tab value="other">その他設定</v-tab>
      </v-tabs>

      <v-divider />

      <v-window v-model="activeTab">
        <v-window-item value="resignation">
          <section class="settings-section">
            <h2>退職ダイアログの文言</h2>
            <FormLayout v-model="message" :schema="resignationMessageSchema">
              <GridBasedForm v-model="message" :fields="resignationMessageFields" />
            </FormLayout>
            <div class="actions">
              <v-btn color="primary" :loading="loading" @click="saveMessage"> 文言を保存 </v-btn>
            </div>
          </section>

          <v-divider />

          <section class="settings-section">
            <div class="section-heading">
              <div>
                <h2>退職時TODO</h2>
                <p>必須にした項目は、確認しないと退職処理を実行できません。</p>
              </div>
              <v-btn color="primary" variant="tonal" @click="openChecklistCreate"> TODO追加 </v-btn>
            </div>

            <v-table density="comfortable">
              <thead>
                <tr>
                  <th>順序</th>
                  <th>コード</th>
                  <th>項目名</th>
                  <th>必須</th>
                  <th>有効</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in checklist" :key="item.id">
                  <td>{{ item.displayOrder }}</td>
                  <td>{{ item.code }}</td>
                  <td>
                    <div class="item-name">{{ item.name }}</div>
                    <div class="item-description">{{ item.description }}</div>
                  </td>
                  <td>{{ item.requiredFlag ? '必須' : '任意' }}</td>
                  <td>{{ item.activeFlag ? '有効' : '無効' }}</td>
                  <td class="row-actions">
                    <v-btn size="small" variant="text" @click="openChecklistEdit(item)">編集</v-btn>
                    <v-btn size="small" color="error" variant="text" @click="removeChecklist(item)"
                      >削除</v-btn
                    >
                  </td>
                </tr>
              </tbody>
            </v-table>
          </section>
        </v-window-item>

        <v-window-item value="closing">
          <section v-if="closingSetting" class="settings-section narrow">
            <h2>給与の締日・支払日</h2>
            <p>ここで保存した設定は月次締め期間と支払予定日の計算に使用されます。</p>
            <DayRuleField v-model="closingSetting.closingDay" label="給与締日" />
            <DayRuleField v-model="closingSetting.paymentDay" label="給与支払日" />
            <div class="actions">
              <v-btn color="primary" :loading="loading" @click="saveClosing">
                締日設定を保存
              </v-btn>
            </div>
          </section>
        </v-window-item>

        <v-window-item value="outputs">
          <section class="settings-section">
            <h2>月次締め帳票</h2>
            <p>
              帳票のレイアウトやジョブは「システム運用 → 帳票管理」で管理し、
              ここでは月次締め時の生成対象と順序を設定します。
            </p>
            <v-table density="comfortable">
              <thead>
                <tr>
                  <th>生成</th>
                  <th>順序</th>
                  <th>帳票</th>
                  <th>形式</th>
                  <th>保存年数</th>
                  <th>必須</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in closingOutputs" :key="item.reportCode">
                  <td><v-checkbox v-model="item.activeFlag" hide-details density="compact" /></td>
                  <td>
                    <v-text-field
                      v-model.number="item.executionOrder"
                      type="number"
                      min="1"
                      hide-details
                      density="compact"
                    />
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
              <v-btn color="primary" :loading="loading" @click="saveOutputs">
                締め帳票設定を保存
              </v-btn>
            </div>
          </section>
        </v-window-item>

        <v-window-item value="backup">
          <section class="settings-section narrow">
            <h2>年度帳票バックアップ</h2>
            <p>
              年度終了後、設定した猶予日数を過ぎてから最初にシステムが起動した時点で、
              保存対象の月次帳票を年度バックアップへコピーします。
            </p>
            <v-alert type="info" variant="tonal">
              指定日時に24時間稼働させる必要はありません。停止中だった場合は、次回起動時に未処理年度を確認します。
            </v-alert>
            <FormLayout v-model="annualReportBackup" :schema="backupSettingSchema">
              <GridBasedForm v-model="annualReportBackup" :fields="backupSettingFields" />
            </FormLayout>
            <div class="actions">
              <v-btn color="primary" :loading="loading" @click="saveBackupSetting">
                バックアップ設定を保存
              </v-btn>
            </div>

            <v-divider />

            <h2>手動実行</h2>
            <p>
              障害時の再確認や初回移行時に、対象年度を指定して実行できます。完了済み年度は重複作成しません。
            </p>
            <div class="manual-backup-row">
              <v-text-field
                v-model.number="manualBackupFiscalYear"
                label="対象年度"
                type="number"
                min="2000"
                max="2200"
                suffix="年度"
                variant="outlined"
                hide-details
              />
              <v-btn color="primary" variant="tonal" :loading="loading" @click="executeBackup">
                今すぐ実行
              </v-btn>
            </div>
            <v-alert
              v-if="lastBackupResult"
              :type="lastBackupResult.status === 'COMPLETED' ? 'success' : 'error'"
              variant="tonal"
            >
              {{ lastBackupResult.fiscalYear }}年度: {{ lastBackupResult.status }} ／
              {{ lastBackupResult.fileCount }}ファイル ／
              {{ lastBackupResult.totalSize.toLocaleString() }} bytes
              <span v-if="lastBackupResult.errorMessage"
                >（{{ lastBackupResult.errorMessage }}）</span
              >
            </v-alert>
          </section>
        </v-window-item>

        <v-window-item value="other">
          <section class="settings-section narrow">
            <h2>サポートリンク</h2>
            <p>
              ヘッダー右上のユーザーメニューから開くリンクを設定します。
              変更内容はログイン済みの全ユーザーに反映されます。
            </p>
            <v-alert type="info" variant="tonal">
              安全のためHTTPSのURLだけを登録できます。リンク先は新しいタブで開きます。
            </v-alert>
            <FormLayout v-model="externalSupportLinks" :schema="supportLinkSchema">
              <GridBasedForm v-model="externalSupportLinks" :fields="supportLinkFields" />
            </FormLayout>
            <div class="actions">
              <v-btn color="primary" :loading="loading" @click="saveExternalSupportLinks">
                その他設定を保存
              </v-btn>
            </div>
          </section>
        </v-window-item>
      </v-window>
    </v-card>

    <template #dialogs>
      <AppDialog
        v-model="checklistDialog"
        :title="editingChecklist.id > 0 ? '退職TODO編集' : '退職TODO追加'"
        size="md"
        body-layout="stack"
        :right-footer-items="checklistFooterItems"
      >
        <FormLayout v-model="editingChecklist" :schema="checklistSchema">
          <GridBasedForm v-model="editingChecklist" :fields="checklistFields" />
        </FormLayout>
      </AppDialog>
    </template>
  </ListDetailPageLayout>
</template>

<style scoped>
.settings-section h2 {
  margin: 0;
}
.settings-section p {
  margin: 6px 0 0;
  color: #64748b;
}
.settings-section {
  display: grid;
  gap: 16px;
  padding: 24px;
}
.settings-section.narrow {
  max-width: 760px;
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
.row-actions {
  white-space: nowrap;
  text-align: right;
}
.item-name {
  font-weight: 700;
}
.item-description {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}
.check-row {
  display: flex;
  gap: 24px;
}
.manual-backup-row {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto;
  align-items: center;
  gap: 12px;
}
</style>
