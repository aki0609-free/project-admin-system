import { computed, ref } from 'vue'
import type { DailyReportTableRow } from '@/features/dailyreport/composables/useDailyReportTableConfig'
import { useDailyReportDetailQuery } from '@/features/dailyreport/api/useDailyReportDetailQuery'

export type DailyReportCreateParams = {
  employeeId?: number
  workDate?: string
}

export const useDailyReportDialog = () => {
  const visible = ref(false)

  const selectedId = ref<number | null>(null)

  const createParams = ref<DailyReportCreateParams | null>(
    null,
  )

  const detailQuery = useDailyReportDetailQuery(
    () => selectedId.value,
  )

  const selected = computed(
    () => detailQuery.dailyReport.value,
  )

  const openCreate = (
    params?: DailyReportCreateParams,
  ) => {
    selectedId.value = null
    createParams.value = params ?? null

    visible.value = true
  }

  const openEdit = (
    row: DailyReportTableRow,
  ) => {
    createParams.value = null

    selectedId.value = row.id
    visible.value = true
  }

  const close = () => {
    visible.value = false
  }

  return {
    visible,

    selectedId,
    selected,

    createParams,

    detailQuery,

    openCreate,
    openEdit,
    close,
  }
}