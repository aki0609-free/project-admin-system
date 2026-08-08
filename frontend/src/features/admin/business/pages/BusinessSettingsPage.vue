<script setup lang="ts">
import DayRuleField from '@/shared/components/form/base/components/form/DayRuleField.vue'
import { useBusinessSettingsPage } from '../composables/useBusinessSettingsPage'

const {
  activeTab,
  loading,
  message,
  checklist,
  closingSetting,
  closingOutputs,
  dormitoryFees,
  checklistDialog,
  editingChecklist,
  saveMessage,
  openChecklistCreate,
  openChecklistEdit,
  saveChecklist,
  removeChecklist,
  saveClosing,
  saveOutputs,
  saveDormitoryFeeSettings,
} = useBusinessSettingsPage()

const dormitoryTypeLabel = (type: 'SINGLE_ROOM' | 'SHARED_ROOM') =>
  type === 'SINGLE_ROOM' ? '一人部屋' : '複数人部屋'
</script>

<template>
  <div class="business-settings-page">
    <header>
      <h1>業務設定</h1>
      <p>退職処理、給与締日、月次締め帳票、寮費を管理します。</p>
    </header>

    <v-card :loading="loading" variant="outlined">
      <v-tabs v-model="activeTab" color="primary">
        <v-tab value="resignation">退職時文言・TODO</v-tab>
        <v-tab value="closing">締日設定</v-tab>
        <v-tab value="outputs">締め帳票</v-tab>
        <v-tab value="dormitory">寮費設定</v-tab>
      </v-tabs>

      <v-divider />

      <v-window v-model="activeTab">
        <v-window-item value="resignation">
          <section class="settings-section">
            <h2>退職ダイアログの文言</h2>
            <v-text-field
              v-model="message.dialogTitle"
              label="ダイアログタイトル"
              variant="outlined"
            />
            <v-textarea
              v-model="message.guidanceMessage"
              label="案内文"
              variant="outlined"
              rows="4"
              auto-grow
            />
            <v-textarea
              v-model="message.confirmationMessage"
              label="警告見出し"
              variant="outlined"
              rows="2"
              auto-grow
            />
            <div class="actions">
              <v-btn color="primary" :loading="loading" @click="saveMessage">
                文言を保存
              </v-btn>
            </div>
          </section>

          <v-divider />

          <section class="settings-section">
            <div class="section-heading">
              <div>
                <h2>退職時TODO</h2>
                <p>必須にした項目は、確認しないと退職処理を実行できません。</p>
              </div>
              <v-btn color="primary" variant="tonal" @click="openChecklistCreate">
                TODO追加
              </v-btn>
            </div>

            <v-table density="comfortable">
              <thead>
                <tr>
                  <th>順序</th><th>コード</th><th>項目名</th><th>必須</th><th>有効</th><th />
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
                    <v-btn size="small" color="error" variant="text" @click="removeChecklist(item)">削除</v-btn>
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
                  <th>生成</th><th>順序</th><th>帳票</th><th>形式</th><th>保存年数</th><th>必須</th>
                </tr>
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
                      max="100"
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

        <v-window-item value="dormitory">
          <section class="settings-section">
            <h2>寮費設定</h2>
            <p>
              部屋タイプ別の日額を設定します。日報の「寮費徴収日数」とRuleを使って、
              土日など日報を作成しない日をまとめて計算できます。
            </p>
            <v-alert type="info" variant="tonal">
              仮設定は0円です。正式な金額を確認してから変更してください。
            </v-alert>
            <v-table density="comfortable">
              <thead>
                <tr>
                  <th>寮タイプ</th>
                  <th>日額</th>
                  <th>有効</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in dormitoryFees" :key="item.dormitoryType">
                  <td>
                    <div class="item-name">{{ dormitoryTypeLabel(item.dormitoryType) }}</div>
                    <div class="item-description">{{ item.dormitoryType }}</div>
                  </td>
                  <td>
                    <v-text-field
                      v-model.number="item.dailyAmount"
                      type="number"
                      min="0"
                      step="1"
                      suffix="円／日"
                      hide-details
                      density="compact"
                    />
                  </td>
                  <td>
                    <v-checkbox v-model="item.activeFlag" hide-details density="compact" />
                  </td>
                </tr>
              </tbody>
            </v-table>
            <div class="actions">
              <v-btn color="primary" :loading="loading" @click="saveDormitoryFeeSettings">
                寮費設定を保存
              </v-btn>
            </div>
          </section>
        </v-window-item>
      </v-window>
    </v-card>

    <v-dialog v-model="checklistDialog" max-width="680">
      <v-card>
        <v-card-title>{{ editingChecklist.id > 0 ? '退職TODO編集' : '退職TODO追加' }}</v-card-title>
        <v-card-text class="dialog-form">
          <v-text-field
            v-model="editingChecklist.code"
            label="TODOコード"
            :readonly="editingChecklist.id > 0"
            variant="outlined"
          />
          <v-text-field v-model="editingChecklist.name" label="項目名" variant="outlined" />
          <v-textarea v-model="editingChecklist.description" label="説明" variant="outlined" rows="3" />
          <v-text-field v-model.number="editingChecklist.displayOrder" label="表示順" type="number" min="0" variant="outlined" />
          <div class="check-row">
            <v-checkbox v-model="editingChecklist.requiredFlag" label="必須項目" hide-details />
            <v-checkbox v-model="editingChecklist.activeFlag" label="有効" hide-details />
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="checklistDialog = false">閉じる</v-btn>
          <v-btn color="primary" :loading="loading" @click="saveChecklist">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.business-settings-page { display: grid; gap: 18px; padding: 20px; }
header h1, .settings-section h2 { margin: 0; }
header p, .settings-section p { margin: 6px 0 0; color: #64748b; }
.settings-section { display: grid; gap: 16px; padding: 24px; }
.settings-section.narrow { max-width: 760px; }
.section-heading { display: flex; align-items: start; justify-content: space-between; gap: 16px; }
.actions { display: flex; justify-content: flex-end; }
.row-actions { white-space: nowrap; text-align: right; }
.item-name { font-weight: 700; }
.item-description { margin-top: 2px; color: #64748b; font-size: 12px; }
.dialog-form { display: grid; gap: 12px; padding-top: 18px !important; }
.check-row { display: flex; gap: 24px; }
</style>
