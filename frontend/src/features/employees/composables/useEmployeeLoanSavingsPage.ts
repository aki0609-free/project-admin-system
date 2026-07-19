import { computed, ref } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useCreateEmployeeLoanMutation } from '../api/useCreateEmployeeLoanMutation'
import { useCreateEmployeeSavingMutation } from '../api/useCreateEmployeeSavingMutation'
import { useDeleteEmployeeLoanMutation } from '../api/useDeleteEmployeeLoanMutation'
import { useDeleteEmployeeSavingMutation } from '../api/useDeleteEmployeeSavingMutation'
import { useEmployeeLoansQuery } from '../api/useEmployeeLoansQuery'
import { useEmployeeSavingsQuery } from '../api/useEmployeeSavingsQuery'
import { useEmployeesQuery } from '../api/useEmployeesQuery'
import { useUpdateEmployeeLoanMutation } from '../api/useUpdateEmployeeLoanMutation'
import { useUpdateEmployeeSavingMutation } from '../api/useUpdateEmployeeSavingMutation'
import { EmployeeLoanForm, EmployeeSavingForm } from '../types/employeeLoanSavingFormTypes'
import { toEmployeeLoanSaveRequest, toEmployeeSavingSaveRequest } from '../utils/employeeLoanSavingConverters'
import { useEmployeeSavingDialog } from './useEmployeeSavingDialog'
import { useEmployeeLoanDialog } from './useEmployeeLoadDialog'

export const useEmployeeLoanSavingsPage = () => {
  const activeTab = ref<'loans' | 'savings'>('loans')

  const employeesQuery = useEmployeesQuery()

  const loansQuery = useEmployeeLoansQuery()
  const createLoanMutation = useCreateEmployeeLoanMutation()
  const updateLoanMutation = useUpdateEmployeeLoanMutation()
  const deleteLoanMutation = useDeleteEmployeeLoanMutation()
  const loanDialog = useEmployeeLoanDialog()

  const savingsQuery = useEmployeeSavingsQuery()
  const createSavingMutation = useCreateEmployeeSavingMutation()
  const updateSavingMutation = useUpdateEmployeeSavingMutation()
  const deleteSavingMutation = useDeleteEmployeeSavingMutation()
  const savingDialog = useEmployeeSavingDialog()

  const busy = computed(
    () =>
      employeesQuery.isLoading.value ||
      loansQuery.isLoading.value ||
      savingsQuery.isLoading.value ||
      createLoanMutation.isPending.value ||
      updateLoanMutation.isPending.value ||
      deleteLoanMutation.isPending.value ||
      createSavingMutation.isPending.value ||
      updateSavingMutation.isPending.value ||
      deleteSavingMutation.isPending.value,
  )

  const loanToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '貸付 新規作成',
      color: 'primary',
      disabled: busy.value,
      onClick: loanDialog.openCreate,
    },
  ])

  const savingToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '貯蓄 新規作成',
      color: 'primary',
      disabled: busy.value,
      onClick: savingDialog.openCreate,
    },
  ])

  const saveLoan = async (form: EmployeeLoanForm) => {
    const request = toEmployeeLoanSaveRequest(form)

    if (form.id > 0) {
      await updateLoanMutation.mutateAsync({
        id: form.id,
        request,
      })
    } else {
      await createLoanMutation.mutateAsync(request)
    }

    loanDialog.close()
  }

  const deleteLoan = async (form: EmployeeLoanForm) => {
    if (form.id <= 0) {
      loanDialog.close()
      return
    }

    const ok = window.confirm('この貸付データを削除しますか？')
    if (!ok) return

    await deleteLoanMutation.mutateAsync(form.id)
    loanDialog.close()
  }

  const saveSaving = async (form: EmployeeSavingForm) => {
    const request = toEmployeeSavingSaveRequest(form)

    if (form.id > 0) {
      await updateSavingMutation.mutateAsync({
        id: form.id,
        request,
      })
    } else {
      await createSavingMutation.mutateAsync(request)
    }

    savingDialog.close()
  }

  const deleteSaving = async (form: EmployeeSavingForm) => {
    if (form.id <= 0) {
      savingDialog.close()
      return
    }

    const ok = window.confirm('この貯蓄データを削除しますか？')
    if (!ok) return

    await deleteSavingMutation.mutateAsync(form.id)
    savingDialog.close()
  }

  const tabs = [
    { label: '貸付', value: 'loans' },
    { label: '貯蓄', value: 'savings' },
  ]

  return {
    activeTab,
    tabs,

    employeesQuery,

    loansQuery,
    loanDialog,
    loanToolbarItems,
    saveLoan,
    deleteLoan,

    savingsQuery,
    savingDialog,
    savingToolbarItems,
    saveSaving,
    deleteSaving,
  }
}