import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import {
  applicantInterviewStatusLabelMap,
  applicantInterviewStatusOptions,
} from '@/features/application/constants/applicantConstants'
import { formatYearMonthDay } from '@/shared/utils/DateUtils'

export const useApplicantMediaColumns = () => {
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
      title: '求人会社',
      key: 'recruitmentCompany',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '媒体種類',
      key: 'mediaType',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '応募媒体',
      key: 'mediaName',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '応募媒体地域',
      key: 'mediaArea',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '応募媒体枠数',
      key: 'mediaSlots',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '募集単価',
      key: 'recruitmentUnitPrice',
      width: '180px',
      filter: { type: 'text' },
      formatter: value =>
        value == null || value === '' ? '' : `${Number(value).toLocaleString()}円`,
    },
  ])

  return {
    columns,
  }
}