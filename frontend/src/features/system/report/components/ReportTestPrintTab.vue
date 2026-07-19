<script setup lang="ts">
import { computed, ref } from 'vue'
import PdfPreviewDialog from '@/shared/components/pdf/PdfPreviewDialog.vue'
import ReportJsonEditor from '@/features/system/report/components/ReportJsonEditor.vue'
import ReportTemplateList from '@/features/system/report/components/ReportTemplateList.vue'
import ReportSelectorPanel from '@/features/system/report/components/ReportSelectorPanel.vue'
import { useReportMastersQuery } from '@/features/system/report/api/queries/useReportMastersQuery'
import { useReportTestPrintJson } from '@/features/system/report/composables/useReportTestPrintJson'
import { useReportTestPrintExecutor } from '@/features/system/report/composables/useReportTestPrintExecutor'
const { reportMasters } = useReportMastersQuery()
const selectedReportId = ref<number | null>(null)
const jsonEditorRef = ref<InstanceType<typeof ReportJsonEditor> | null>(null)
const selectedReport = computed(
  () => reportMasters.value.find((x) => x.id === selectedReportId.value) ?? null,
)
const selectedReportCode = computed(() => selectedReport.value?.reportCode ?? null)

const {
  paramsText,
  templateName,
  hasJsonError,
  jsonErrorMessage,
  apiParams,
  templates,
  isSavingTemplate,
  saveCurrentJson,
  applyTemplate,
  generateSampleJson,
} = useReportTestPrintJson(selectedReportCode)

const { previewVisible, pdfUrl, execute, isPending } = useReportTestPrintExecutor()

const formatJson = async () => {
  await jsonEditorRef.value?.format()
}

const executeTestPrint = async () => {
  if (!selectedReport.value) {
    alert('帳票を選択してください。')
    return
  }

  if (hasJsonError.value) {
    alert('JSON形式が不正です。')
    return
  }

  await execute(selectedReport.value.reportCode, apiParams.value)
}

const handleSendMail = () => {
  alert('メール送信（ダミー）')
}
</script>

<template>
  <div class="test-print-tab">
    <v-card class="pa-4" rounded="lg" variant="outlined">
      <div class="form-grid">
        <ReportSelectorPanel
          v-model:selected-report-id="selectedReportId"
          v-model:template-name="templateName"
          :report-masters="reportMasters"
          :selected-report="selectedReport"
        />

        <div class="editor-header">
          <div>
            <div class="editor-title">テストパラメータ(JSON)</div>

            <div class="editor-subtitle">
              画面上は reportCode と params を含めて編集できます。 API送信時は params
              の中身のみ送信します。
            </div>
          </div>

          <div class="editor-actions">
            <v-btn variant="outlined" size="small" @click="generateSampleJson">
              サンプル生成
            </v-btn>

            <v-btn variant="outlined" size="small" @click="formatJson"> 整形 </v-btn>

            <v-btn
              variant="outlined"
              size="small"
              color="primary"
              :loading="isSavingTemplate"
              :disabled="isSavingTemplate"
              @click="saveCurrentJson"
            >
              保存
            </v-btn>
          </div>
        </div>

        <ReportJsonEditor
          ref="jsonEditorRef"
          v-model="paramsText"
          :error-message="jsonErrorMessage"
          @save="saveCurrentJson"
        />

        <ReportTemplateList :templates="templates" @select="applyTemplate" />
      </div>

      <div class="actions">
        <v-btn
          color="primary"
          variant="elevated"
          :loading="isPending"
          :disabled="isPending || hasJsonError"
          @click="executeTestPrint"
        >
          印刷
        </v-btn>
      </div>
    </v-card>

    <PdfPreviewDialog
      v-model="previewVisible"
      title="テスト印刷プレビュー"
      :pdf-url="pdfUrl"
      @send-mail="handleSendMail"
    />
  </div>
</template>

<style scoped>
.test-print-tab {
  display: grid;
  gap: 12px;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
  padding-right: 4px;
}

.form-grid {
  display: grid;
  gap: 16px;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 12px;
}

.editor-title {
  font-weight: 700;
}

.editor-subtitle {
  font-size: 12px;
  color: #64748b;
}

.editor-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;

  position: sticky;
  bottom: 0;

  background: #fff;

  padding: 12px 0 0;
}

@media (max-width: 900px) {
  .editor-header {
    display: grid;
  }
}
</style>
