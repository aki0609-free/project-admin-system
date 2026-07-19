import { computed } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useDailyReportsQuery } from '@/features/dailyreport/api/useDailyReportsQuery'
import { useUpdateDailyReportMutation } from '@/features/dailyreport/api/useUpdateDailyReportMutation'
import { useDeleteDailyReportMutation } from '@/features/dailyreport/api/useDeleteDailyReportMutation'
import { useDailyReportDialog } from '@/features/dailyreport/composables/useDailyReportDialog'
import { toDailyReportSaveRequest } from '@/features/dailyreport/utils/dailyReportConverters'
import type { DailyReportForm } from '@/features/dailyreport/types/dailyReportFormTypes'
import { useEmployeesQuery } from '@/features/employees/api/useEmployeesQuery'
import { useCreateDailyReportMutation } from '../api/useCreateDailyRportMutation'

export const useDailyReportPage = () => {
  const employeesQuery = useEmployeesQuery()
  const dailyReportsQuery = useDailyReportsQuery()

  const createMutation = useCreateDailyReportMutation()
  const updateMutation = useUpdateDailyReportMutation()
  const deleteMutation = useDeleteDailyReportMutation()

  const dialog = useDailyReportDialog()

  const busy = computed(
    () =>
      employeesQuery.isLoading.value ||
      dailyReportsQuery.isLoading.value ||
      createMutation.isPending.value ||
      updateMutation.isPending.value ||
      deleteMutation.isPending.value,
  )

  const leftToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '新規作成',
      color: 'primary',
      disabled: busy.value,
      onClick: () => dialog.openCreate(),
    },
  ])

  const rightToolbarItems = [] as ToolbarItem[]

  const save = async (form: DailyReportForm) => {
    const request = toDailyReportSaveRequest(form)

    if (form.id > 0) {
      await updateMutation.mutateAsync({
        id: form.id,
        request,
      })
    } else {
      await createMutation.mutateAsync(request)
    }

    dialog.close()
  }

  const remove = async (form: DailyReportForm) => {
    if (form.id <= 0) {
      dialog.close()
      return
    }

    const ok = window.confirm('この日報を削除しますか？')
    if (!ok) return

    await deleteMutation.mutateAsync(form.id)
    dialog.close()
  }

  return {
    employeesQuery,
    dailyReportsQuery,

    dialog,

    busy,

    leftToolbarItems,
    rightToolbarItems,

    save,
    remove,
  }
}