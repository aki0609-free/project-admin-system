import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import { applicantRecruitmentStatusLabelMap, applicantRecruitmentStatusOptions } from '@/features/application/constants/applicantConstants'
import { formatYearMonthDay } from '@/shared/utils/DateUtils'

export const useApplicantRecruitmentColumns = () => {
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
      title: '問い合わせ日',
      key: 'contactDate',
      width: '180px',
      filter: { type: 'date' },
      formatter: (value, row) => formatYearMonthDay(value as string)
    },
    {
      title: '採用状況',
      key: 'recruitmentStatus',
      filter: { type: 'select' },
      width: '180px',
      enumOptions: [...applicantRecruitmentStatusOptions],
      formatter: value =>
        applicantRecruitmentStatusLabelMap[
          value as keyof typeof applicantRecruitmentStatusLabelMap
        ] ?? '',
    },
    {
      title: 'どこで',
      key: 'sourceWhere',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '検索サイト',
      key: 'searchSite',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: 'キーワード',
      key: 'keyword',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '転載先',
      key: 'repostedTo',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '応募理由1',
      key: 'applicationReason1',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '応募理由2',
      key: 'applicationReason2',
      width: '180px',
      filter: { type: 'text' },
    },
  ])

  return {
    columns,
  }
}