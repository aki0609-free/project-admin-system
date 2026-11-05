import { onMounted, reactive, ref } from 'vue'
import {
  getClosingOutputs,
  getPreviewReportSettings,
  saveClosingOutputs,
  savePreviewReportSetting,
} from '../api/reportOperationSettingApi'
import type {
  MonthlyClosingOutputSetting,
  PreviewReportSetting,
  PreviewReportSettingSaveRequest,
} from '../types/reportOperationSettingTypes'

const emptyPreviewReport = (): PreviewReportSetting => ({
  id: null,
  operationType: 'DAILY',
  reportCode: '',
  reportName: '',
  tableName: '',
  filterColumnName: null,
  targetParamName: null,
  orderBy: null,
  displayOrder: 10,
  outputType: 'HTML_PREVIEW',
  activeFlag: true,
  htmlTemplateKey: null,
  htmlTemplateVersion: 1,
  htmlTemplateHash: null,
  templateExists: false,
})

export const useReportOperationSettings = () => {
  const loading = ref(false)
  const errorMessage = ref('')
  const successMessage = ref('')
  const closingOutputs = ref<MonthlyClosingOutputSetting[]>([])
  const previewReports = ref<PreviewReportSetting[]>([])
  const previewReportDialog = ref(false)
  const editingPreviewReport = reactive<PreviewReportSetting>(emptyPreviewReport())
  const previewTemplateFile = ref<File | null>(null)

  const run = async (action: () => Promise<void>, successText?: string) => {
    loading.value = true
    errorMessage.value = ''
    try {
      await action()
      if (successText) successMessage.value = successText
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '処理に失敗しました。'
    } finally {
      loading.value = false
    }
  }

  const load = () => run(async () => {
    const [outputs, previews] = await Promise.all([
      getClosingOutputs(),
      getPreviewReportSettings(),
    ])
    closingOutputs.value = outputs
    previewReports.value = previews
  })

  const saveOutputs = () => run(async () => {
    closingOutputs.value = await saveClosingOutputs(closingOutputs.value.map((item) => ({
      reportCode: item.reportCode,
      executionOrder: Number(item.executionOrder),
      activeFlag: item.activeFlag,
      backupRetentionYears: item.backupRetentionYears == null
        ? null
        : Number(item.backupRetentionYears),
    })))
  }, '締め帳票設定を保存しました。')

  const openPreviewReportCreate = () => {
    Object.assign(editingPreviewReport, emptyPreviewReport())
    previewTemplateFile.value = null
    previewReportDialog.value = true
  }

  const openPreviewReportEdit = (item: PreviewReportSetting) => {
    Object.assign(editingPreviewReport, item)
    previewTemplateFile.value = null
    previewReportDialog.value = true
  }

  const savePreviewReport = () => {
    if (!editingPreviewReport.reportCode.trim()
      || !editingPreviewReport.reportName.trim()
      || !editingPreviewReport.tableName.trim()) {
      errorMessage.value = '帳票コード、帳票名、データ元View/Tableは必須です。'
      return
    }
    if (editingPreviewReport.id == null && !previewTemplateFile.value) {
      errorMessage.value = '新規登録時はHTMLテンプレートを選択してください。'
      return
    }
    return run(async () => {
      const request: PreviewReportSettingSaveRequest = {
        id: editingPreviewReport.id,
        operationType: editingPreviewReport.operationType,
        reportCode: editingPreviewReport.reportCode.trim(),
        reportName: editingPreviewReport.reportName.trim(),
        tableName: editingPreviewReport.tableName.trim(),
        filterColumnName: editingPreviewReport.filterColumnName?.trim() || null,
        targetParamName: editingPreviewReport.targetParamName?.trim() || null,
        orderBy: editingPreviewReport.orderBy?.trim() || null,
        displayOrder: Number(editingPreviewReport.displayOrder),
        outputType: editingPreviewReport.outputType,
        activeFlag: editingPreviewReport.activeFlag,
      }
      await savePreviewReportSetting(request, previewTemplateFile.value)
      previewReports.value = await getPreviewReportSettings()
      previewReportDialog.value = false
    }, 'プレビュー帳票を保存しました。')
  }

  onMounted(() => void load())

  return {
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
  }
}
