import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  createResignationChecklist,
  deleteResignationChecklist,
  executeAnnualReportBackup,
  getAnnualReportBackupSetting,
  getClosingOutputs,
  getClosingSetting,
  getExternalSupportLinkSetting,
  getResignationChecklist,
  getResignationMessage,
  saveClosingOutputs,
  saveClosingSetting,
  saveAnnualReportBackupSetting,
  saveExternalSupportLinkSetting,
  saveResignationMessage,
  updateResignationChecklist,
} from '../api/businessSettingApi'
import type {
  AnnualReportBackupResult,
  AnnualReportBackupSetting,
  BusinessClosingSetting,
  ExternalSupportLinkSetting,
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
  const activeTab = ref<'resignation' | 'closing' | 'outputs' | 'backup' | 'other'>(
    'resignation',
  )
  const loading = ref(false)
  const errorMessage = ref('')
  const successMessage = ref('')
  let successMessageTimer: ReturnType<typeof setTimeout> | undefined
  const message = reactive<ResignationMessage>({
    dialogTitle: '退職処理',
    guidanceMessage: '',
    confirmationMessage: '',
  })
  const checklist = ref<ResignationChecklistItem[]>([])
  const closingSetting = ref<BusinessClosingSetting | null>(null)
  const closingOutputs = ref<MonthlyClosingOutputSetting[]>([])
  const annualReportBackup = reactive<AnnualReportBackupSetting>({
    fiscalYearStartMonth: 4,
    graceDays: 14,
    startupEnabled: true,
    activeFlag: true,
  })
  const externalSupportLinks = reactive<ExternalSupportLinkSetting>({
    incidentReportUrl: '',
    manualUrl: '',
  })
  const manualBackupFiscalYear = ref(new Date().getFullYear())
  const lastBackupResult = ref<AnnualReportBackupResult | null>(null)
  const checklistDialog = ref(false)
  const editingChecklist = reactive<ResignationChecklistItem>(emptyChecklist())

  const showSuccess = (text: string) => {
    successMessage.value = text
    if (successMessageTimer) clearTimeout(successMessageTimer)
    successMessageTimer = setTimeout(() => {
      successMessage.value = ''
      successMessageTimer = undefined
    }, 4000)
  }

  const run = async (action: () => Promise<void>, successText?: string) => {
    loading.value = true
    errorMessage.value = ''
    try {
      await action()
      if (successText) showSuccess(successText)
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '処理に失敗しました。'
    } finally {
      loading.value = false
    }
  }

  const load = () =>
    run(async () => {
      const failures: string[] = []
      await Promise.all([
        getResignationMessage()
          .then((value) => Object.assign(message, value))
          .catch(() => failures.push('退職時文言')),
        getResignationChecklist()
          .then((value) => {
            checklist.value = value
          })
          .catch(() => failures.push('退職時TODO')),
        getClosingSetting()
          .then((value) => {
            closingSetting.value = value
          })
          .catch(() => failures.push('締日設定')),
        getClosingOutputs()
          .then((value) => {
            closingOutputs.value = value
          })
          .catch(() => failures.push('締め帳票')),
        getAnnualReportBackupSetting()
          .then((value) => Object.assign(annualReportBackup, value))
          .catch(() => failures.push('帳票バックアップ')),
        getExternalSupportLinkSetting()
          .then((value) => Object.assign(externalSupportLinks, value))
          .catch(() => failures.push('その他設定')),
      ])
      if (failures.length > 0) {
        throw new Error(`設定の取得に失敗しました: ${failures.join('、')}`)
      }
      const now = new Date()
      manualBackupFiscalYear.value =
        now.getFullYear() - (now.getMonth() + 1 < annualReportBackup.fiscalYearStartMonth ? 1 : 0)
    })

  const saveMessage = () =>
    run(async () => {
      Object.assign(message, await saveResignationMessage({ ...message }))
    }, '退職時文言を保存しました。')

  const openChecklistCreate = () => {
    Object.assign(editingChecklist, emptyChecklist())
    checklistDialog.value = true
  }

  const openChecklistEdit = (item: ResignationChecklistItem) => {
    Object.assign(editingChecklist, item)
    checklistDialog.value = true
  }

  const saveChecklist = () =>
    run(async () => {
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
    }, '退職時TODOを保存しました。')

  const removeChecklist = (item: ResignationChecklistItem) => {
    if (!window.confirm(`「${item.name}」を削除しますか？`)) return
    return run(async () => {
      await deleteResignationChecklist(item.id)
      checklist.value = await getResignationChecklist()
    }, '退職時TODOを削除しました。')
  }

  const saveClosing = () =>
    run(async () => {
      if (!closingSetting.value) return
      closingSetting.value = await saveClosingSetting({
        closingDay: closingSetting.value.closingDay,
        paymentDay: closingSetting.value.paymentDay,
      })
    }, '締日設定を保存しました。')

  const saveOutputs = () =>
    run(async () => {
      closingOutputs.value = await saveClosingOutputs(
        closingOutputs.value.map((item) => ({
          reportCode: item.reportCode,
          executionOrder: Number(item.executionOrder),
          activeFlag: item.activeFlag,
          backupRetentionYears:
            item.backupRetentionYears == null ? null : Number(item.backupRetentionYears),
        })),
      )
    }, '締め帳票設定を保存しました。')

  const saveBackupSetting = () =>
    run(async () => {
      Object.assign(
        annualReportBackup,
        await saveAnnualReportBackupSetting({
          fiscalYearStartMonth: Number(annualReportBackup.fiscalYearStartMonth),
          graceDays: Number(annualReportBackup.graceDays),
          startupEnabled: annualReportBackup.startupEnabled,
          activeFlag: annualReportBackup.activeFlag,
        }),
      )
    }, '帳票バックアップ設定を保存しました。')

  const executeBackup = () => {
    const fiscalYear = Number(manualBackupFiscalYear.value)
    if (!window.confirm(`${fiscalYear}年度の帳票バックアップを実行しますか？`)) return
    return run(async () => {
      lastBackupResult.value = await executeAnnualReportBackup(fiscalYear)
    }, `${fiscalYear}年度の帳票バックアップを実行しました。`)
  }

  const saveExternalSupportLinks = () =>
    run(async () => {
      Object.assign(
        externalSupportLinks,
        await saveExternalSupportLinkSetting({ ...externalSupportLinks }),
      )
    }, 'その他設定を保存しました。')

  onMounted(() => {
    void load()
  })
  onBeforeUnmount(() => {
    if (successMessageTimer) clearTimeout(successMessageTimer)
  })

  return {
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
    load,
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
  }
}
