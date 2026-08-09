import { onMounted, reactive, ref } from 'vue'
import {
  createResignationChecklist,
  deleteResignationChecklist,
  executeAnnualReportBackup,
  getAnnualReportBackupSetting,
  getClosingOutputs,
  getClosingSetting,
  getDormitoryFees,
  getResignationChecklist,
  getResignationMessage,
  saveClosingOutputs,
  saveClosingSetting,
  saveAnnualReportBackupSetting,
  saveDormitoryFees,
  saveResignationMessage,
  updateResignationChecklist,
} from '../api/businessSettingApi'
import type {
  AnnualReportBackupResult,
  AnnualReportBackupSetting,
  BusinessClosingSetting,
  DormitoryFeeSetting,
  MonthlyClosingOutputSetting,
  ResignationChecklistItem,
  ResignationChecklistSaveRequest,
  ResignationMessage,
} from '../types/businessSettingTypes'

const emptyChecklist = (): ResignationChecklistItem => ({
  id: 0,
  code: '',
  name: '',
  description: null,
  requiredFlag: true,
  displayOrder: 10,
  activeFlag: true,
})

export const useBusinessSettingsPage = () => {
  const activeTab = ref<'resignation' | 'closing' | 'outputs' | 'backup' | 'dormitory'>('resignation')
  const loading = ref(false)
  const message = reactive<ResignationMessage>({
    dialogTitle: '退職処理',
    guidanceMessage: '',
    confirmationMessage: '',
  })
  const checklist = ref<ResignationChecklistItem[]>([])
  const closingSetting = ref<BusinessClosingSetting | null>(null)
  const closingOutputs = ref<MonthlyClosingOutputSetting[]>([])
  const dormitoryFees = ref<DormitoryFeeSetting[]>([])
  const annualReportBackup = reactive<AnnualReportBackupSetting>({
    fiscalYearStartMonth: 4,
    graceDays: 14,
    startupEnabled: true,
    activeFlag: true,
  })
  const manualBackupFiscalYear = ref(new Date().getFullYear())
  const lastBackupResult = ref<AnnualReportBackupResult | null>(null)
  const checklistDialog = ref(false)
  const editingChecklist = reactive<ResignationChecklistItem>(emptyChecklist())

  const run = async (action: () => Promise<void>) => {
    loading.value = true
    try {
      await action()
    } catch (error) {
      const text = error instanceof Error ? error.message : '処理に失敗しました。'
      window.alert(text)
      throw error
    } finally {
      loading.value = false
    }
  }

  const load = () => run(async () => {
    const [loadedMessage, loadedChecklist, loadedClosing, loadedOutputs, loadedDormitoryFees, loadedBackup] =
      await Promise.all([
        getResignationMessage(),
        getResignationChecklist(),
        getClosingSetting(),
        getClosingOutputs(),
        getDormitoryFees(),
        getAnnualReportBackupSetting(),
      ])
    Object.assign(message, loadedMessage)
    checklist.value = loadedChecklist
    closingSetting.value = loadedClosing
    closingOutputs.value = loadedOutputs
    dormitoryFees.value = loadedDormitoryFees
    Object.assign(annualReportBackup, loadedBackup)
    const now = new Date()
    manualBackupFiscalYear.value = now.getFullYear()
      - (now.getMonth() + 1 < loadedBackup.fiscalYearStartMonth ? 1 : 0)
  })

  const saveMessage = () => run(async () => {
    Object.assign(message, await saveResignationMessage({ ...message }))
  })

  const openChecklistCreate = () => {
    Object.assign(editingChecklist, emptyChecklist())
    checklistDialog.value = true
  }

  const openChecklistEdit = (item: ResignationChecklistItem) => {
    Object.assign(editingChecklist, item)
    checklistDialog.value = true
  }

  const saveChecklist = () => run(async () => {
    const request: ResignationChecklistSaveRequest = {
      code: editingChecklist.code,
      name: editingChecklist.name,
      description: editingChecklist.description,
      requiredFlag: editingChecklist.requiredFlag,
      displayOrder: Number(editingChecklist.displayOrder),
      activeFlag: editingChecklist.activeFlag,
    }
    if (editingChecklist.id > 0) {
      await updateResignationChecklist(editingChecklist.id, request)
    } else {
      await createResignationChecklist(request)
    }
    checklist.value = await getResignationChecklist()
    checklistDialog.value = false
  })

  const removeChecklist = (item: ResignationChecklistItem) => {
    if (!window.confirm(`「${item.name}」を削除しますか？`)) return
    return run(async () => {
      await deleteResignationChecklist(item.id)
      checklist.value = await getResignationChecklist()
    })
  }

  const saveClosing = () => run(async () => {
    if (!closingSetting.value) return
    closingSetting.value = await saveClosingSetting({
      closingDay: closingSetting.value.closingDay,
      paymentDay: closingSetting.value.paymentDay,
    })
  })

  const saveOutputs = () => run(async () => {
    closingOutputs.value = await saveClosingOutputs(
      closingOutputs.value.map((item) => ({
        reportCode: item.reportCode,
        executionOrder: Number(item.executionOrder),
        activeFlag: item.activeFlag,
        backupRetentionYears: item.backupRetentionYears == null
          ? null
          : Number(item.backupRetentionYears),
      })),
    )
  })

  const saveDormitoryFeeSettings = () => run(async () => {
    dormitoryFees.value = await saveDormitoryFees(
      dormitoryFees.value.map((item) => ({
        dormitoryType: item.dormitoryType,
        dailyAmount: Number(item.dailyAmount),
        activeFlag: item.activeFlag,
      })),
    )
  })

  const saveBackupSetting = () => run(async () => {
    Object.assign(annualReportBackup, await saveAnnualReportBackupSetting({
      fiscalYearStartMonth: Number(annualReportBackup.fiscalYearStartMonth),
      graceDays: Number(annualReportBackup.graceDays),
      startupEnabled: annualReportBackup.startupEnabled,
      activeFlag: annualReportBackup.activeFlag,
    }))
  })

  const executeBackup = () => {
    const fiscalYear = Number(manualBackupFiscalYear.value)
    if (!window.confirm(`${fiscalYear}年度の帳票バックアップを実行しますか？`)) return
    return run(async () => {
      lastBackupResult.value = await executeAnnualReportBackup(fiscalYear)
    })
  }

  onMounted(() => { void load() })

  return {
    activeTab,
    loading,
    message,
    checklist,
    closingSetting,
    closingOutputs,
    dormitoryFees,
    annualReportBackup,
    manualBackupFiscalYear,
    lastBackupResult,
    checklistDialog,
    editingChecklist,
    load,
    saveMessage,
    openChecklistCreate,
    openChecklistEdit,
    saveChecklist,
    removeChecklist,
    saveClosing,
    saveOutputs,
    saveDormitoryFeeSettings,
    saveBackupSetting,
    executeBackup,
  }
}
