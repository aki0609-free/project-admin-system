import { watch, type Ref } from 'vue'

import { useDailyReportInputItemsQuery } from '@/features/dailyreport/api/useDailyReportInputItemsQuery'
import type { DailyReportForm } from '@/features/dailyreport/types/dailyReportFormTypes'
import type {
  DailyReportAmountItemForm,
  DailyReportInputItemResponse,
} from '@/features/dailyreport/types/dailyReportInputItemTypes'

const toAmountItemForm = (
  item: DailyReportInputItemResponse,
): DailyReportAmountItemForm => ({
  masterId: item.masterId,
  code: item.code,
  name: item.name,
  itemType: item.itemType,
  inputMode: item.inputMode,
  amount: item.amount ?? 0,
  editable: item.editable,
  displayOrder: item.displayOrder,
})

export const useDailyReportInputItems = (
  form: Ref<DailyReportForm>,
  enabled: Ref<boolean>,
) => {
  const query = useDailyReportInputItemsQuery()

  watch(
    () => query.inputItems.value,
    (data) => {
      if (!enabled.value) {
        return
      }

      if (form.value.employeeId == null || !form.value.workDate.trim()) {
        return
      }

      form.value.allowances = data.allowances.map(toAmountItemForm)
      form.value.deductions = data.deductions.map(toAmountItemForm)
    },
    { immediate: true },
  )

  return query
}