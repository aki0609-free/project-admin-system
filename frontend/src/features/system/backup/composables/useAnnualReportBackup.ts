import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  executeAnnualReportBackup,
  getAnnualReportBackupSetting,
  saveAnnualReportBackupSetting,
} from '../api/annualReportBackupApi'
import type {
  AnnualReportBackupResult,
  AnnualReportBackupSetting,
} from '../types/annualReportBackupTypes'

export const useAnnualReportBackup = () => {
  const loading = ref(false)
  const errorMessage = ref('')
  const successMessage = ref('')
  const setting = reactive<AnnualReportBackupSetting>({
    fiscalYearStartMonth: 4,
    graceDays: 14,
    startupEnabled: true,
    activeFlag: true,
  })
  const manualFiscalYear = ref(new Date().getFullYear())
  const lastResult = ref<AnnualReportBackupResult | null>(null)
  let successMessageTimer: ReturnType<typeof setTimeout> | undefined

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

  const load = () => run(async () => {
    Object.assign(setting, await getAnnualReportBackupSetting())
    const now = new Date()
    manualFiscalYear.value =
      now.getFullYear() - (now.getMonth() + 1 < setting.fiscalYearStartMonth ? 1 : 0)
  })

  const save = () => run(async () => {
    Object.assign(setting, await saveAnnualReportBackupSetting({
      fiscalYearStartMonth: Number(setting.fiscalYearStartMonth),
      graceDays: Number(setting.graceDays),
      startupEnabled: setting.startupEnabled,
      activeFlag: setting.activeFlag,
    }))
  }, '年度帳票バックアップ設定を保存しました。')

  const execute = () => {
    const fiscalYear = Number(manualFiscalYear.value)
    if (!Number.isInteger(fiscalYear) || fiscalYear < 2000 || fiscalYear > 2200) {
      errorMessage.value = '対象年度は2000〜2200の整数で指定してください。'
      return
    }
    if (!window.confirm(`${fiscalYear}年度の帳票バックアップを実行しますか？`)) return
    return run(async () => {
      const result = await executeAnnualReportBackup(fiscalYear)
      lastResult.value = result
      if (result.status !== 'COMPLETED') {
        throw new Error(result.errorMessage || `${fiscalYear}年度の帳票バックアップに失敗しました。`)
      }
      showSuccess(`${fiscalYear}年度の帳票バックアップが完了しました。`)
    })
  }

  onMounted(() => void load())
  onBeforeUnmount(() => {
    if (successMessageTimer) clearTimeout(successMessageTimer)
  })

  return {
    loading,
    errorMessage,
    successMessage,
    setting,
    manualFiscalYear,
    lastResult,
    save,
    execute,
  }
}
