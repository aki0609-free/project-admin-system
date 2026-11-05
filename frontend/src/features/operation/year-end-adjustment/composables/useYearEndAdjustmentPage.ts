import { onMounted, reactive, ref } from 'vue'
import { get } from '@/shared/api/http'
import type { EmployeeListItemResponse } from '@/features/employees/types/employeeApiTypes'
import {
  deleteYearEndAdjustment,
  getYearEndAdjustments,
  saveYearEndAdjustment,
} from '../api/yearEndAdjustmentApi'
import type { YearEndAdjustment } from '../types/yearEndAdjustmentTypes'

const emptyAdjustment = (taxYear: number): YearEndAdjustment => ({
  id: null,
  employeeId: null,
  taxYear,
  targetMonth: `${taxYear}-12`,
  adjustmentAmount: 0,
  status: 'DRAFT',
  note: null,
  lockVersion: null,
})

export const useYearEndAdjustmentPage = () => {
  const loading = ref(false)
  const errorMessage = ref('')
  const successMessage = ref('')
  const taxYear = ref(new Date().getFullYear())
  const adjustments = ref<YearEndAdjustment[]>([])
  const employees = ref<EmployeeListItemResponse[]>([])
  const editing = reactive<YearEndAdjustment>(emptyAdjustment(taxYear.value))

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

  const resetEditor = () => {
    Object.assign(editing, emptyAdjustment(Number(taxYear.value)))
  }

  const loadAdjustments = () => run(async () => {
    adjustments.value = await getYearEndAdjustments(Number(taxYear.value))
    resetEditor()
  })

  const editAdjustment = (item?: YearEndAdjustment) => {
    Object.assign(editing, item ?? emptyAdjustment(Number(taxYear.value)))
  }

  const saveAdjustment = () => {
    if (editing.employeeId == null) {
      errorMessage.value = '従業員を選択してください。'
      return
    }
    return run(async () => {
      await saveYearEndAdjustment({
        ...editing,
        taxYear: Number(taxYear.value),
        adjustmentAmount: Number(editing.adjustmentAmount),
      })
      adjustments.value = await getYearEndAdjustments(Number(taxYear.value))
      resetEditor()
    }, '年末調整を保存しました。')
  }

  const removeAdjustment = (item: YearEndAdjustment) => {
    if (item.id == null || !window.confirm(`${item.employeeName ?? ''}の年末調整を削除しますか？`)) return
    return run(async () => {
      await deleteYearEndAdjustment(item.id as number)
      adjustments.value = await getYearEndAdjustments(Number(taxYear.value))
    }, '年末調整を削除しました。')
  }

  onMounted(() => {
    void run(async () => {
      const [employeeValues, adjustmentValues] = await Promise.all([
        get<EmployeeListItemResponse[]>('/api/employees'),
        getYearEndAdjustments(taxYear.value),
      ])
      employees.value = employeeValues
      adjustments.value = adjustmentValues
    })
  })

  return {
    loading,
    errorMessage,
    successMessage,
    taxYear,
    adjustments,
    employees,
    editing,
    loadAdjustments,
    editAdjustment,
    saveAdjustment,
    removeAdjustment,
  }
}
