import { onMounted, reactive, ref } from 'vue'
import {
  createResignationChecklist,
  deleteResignationChecklist,
  getClosingOutputs,
  getClosingSetting,
  getDormitoryFees,
  getResignationChecklist,
  getResignationMessage,
  saveClosingOutputs,
  saveClosingSetting,
  saveDormitoryFees,
  saveResignationMessage,
  updateResignationChecklist,
} from '../api/businessSettingApi'
import type {
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
  const activeTab = ref<'resignation' | 'closing' | 'outputs' | 'dormitory'>('resignation')
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
    const [loadedMessage, loadedChecklist, loadedClosing, loadedOutputs, loadedDormitoryFees] =
      await Promise.all([
        getResignationMessage(),
        getResignationChecklist(),
        getClosingSetting(),
        getClosingOutputs(),
        getDormitoryFees(),
      ])
    Object.assign(message, loadedMessage)
    checklist.value = loadedChecklist
    closingSetting.value = loadedClosing
    closingOutputs.value = loadedOutputs
    dormitoryFees.value = loadedDormitoryFees
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

  onMounted(() => { void load() })

  return {
    activeTab,
    loading,
    message,
    checklist,
    closingSetting,
    closingOutputs,
    dormitoryFees,
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
  }
}
