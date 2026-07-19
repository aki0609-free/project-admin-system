import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import { formatYearMonthDay } from '@/shared/utils/DateUtils'
import { formatTenureFromDates } from '@/shared/utils/TenureUtils'

export const useApplicantEmploymentColumns = () => {
  const columns = computed<SimpleTableColumnDef<ApplicantRow>[]>(() => [
    {
      title: 'No',
      key: 'no',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '氏名',
      key: 'name',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '入社日(社保無)',
      key: 'joinDateWithoutInsurance',
      width: '180px',
      filter: { type: 'date' },
      formatter: (value, row) => formatYearMonthDay(value as string),
    },
    {
      title: '退職日(社保無)',
      key: 'leaveDateWithoutInsurance',
      width: '180px',
      filter: { type: 'date' },
      formatter: (value, row) => formatYearMonthDay(value as string),
    },
    {
      title: '在籍期間(社保無)',
      key: 'tenureWithoutInsurance',
      width: '180px',
      filter: { type: 'text' },
      computed: true,
        valueGetter: (row) =>
          formatTenureFromDates(
            row.joinDateWithoutInsurance,
            row.leaveDateWithoutInsurance,
          ),
    },
    {
      title: '入社日(社保有)',
      key: 'joinDateWithInsurance',
      width: '180px',
      filter: { type: 'date' },
      formatter: (value, row) => formatYearMonthDay(value as string),
    },
    {
      title: '退職日(社保有)',
      key: 'leaveDateWithInsurance',
      width: '180px',
      filter: { type: 'date' },
      formatter: (value, row) => formatYearMonthDay(value as string),
    },
    {
      title: '在籍期間(社保有)',
      key: 'tenureWithInsurance',
      width: '180px',
      filter: { type: 'text' },
      computed: true,
      valueGetter: (row) =>
        formatTenureFromDates(
          row.joinDateWithInsurance,
          row.leaveDateWithInsurance,
        ),
    },
    {
      title: '退職前得意先',
      key: 'clientBeforeLeave',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '退職前業種',
      key: 'industryBeforeLeave',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '退職理由（想定）',
      key: 'estimatedRetirementReason',
      filter: { type: 'text' },
      width: '180px',
    },
  ])

  return {
    columns,
  }
}
