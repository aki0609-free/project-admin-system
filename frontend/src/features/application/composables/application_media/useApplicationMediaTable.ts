import { computed, type ComputedRef } from 'vue'
import type {
  MultiLevelHeaderColumnDef,
  MultiLevelHeaderGroupColumn,
  MultiLevelHeaderTableDef,
} from '@/shared/components/table/multi_level_header_table/types/item/types'
import type { ApplicationMediaPivotRow } from '../../types/applicationMediaTypes'
import { formatYearMonth } from '@/shared/utils/DateUtils'

export const useApplicationMediaTable = (
  yearMonths: ComputedRef<string[]>,
) => {
  const columns = computed<MultiLevelHeaderColumnDef<ApplicationMediaPivotRow>[]>(() =>
    yearMonths.value.map(
      (yearMonth): MultiLevelHeaderGroupColumn<ApplicationMediaPivotRow> => ({
        key: yearMonth,
        title: formatYearMonth(yearMonth),
        subColumns: [
          {
            key: `${yearMonth}_mediaArea`,
            title: '掲載地域',
            type: 'text',
            editable: true,
            width: '180px',
          },
          {
            key: `${yearMonth}_mediaSlots`,
            title: '掲載枠',
            type: 'number',
            editable: true,
            aggregation: 'sum',
            width: '100px',
          },
          {
            key: `${yearMonth}_cost`,
            title: 'コスト',
            type: 'number',
            editable: true,
            formatter: (v: unknown) =>
              v == null || v === '' ? '' : `${Number(v).toLocaleString()}円`,
            aggregation: 'sum',
            width: '140px',
          },
          {
            key: `${yearMonth}_hires`,
            title: '採用数',
            type: 'number',
            editable: false,
            aggregation: 'sum',
            width: '100px',
          },
          {
            key: `${yearMonth}_unitPrice`,
            title: '単価',
            type: 'number',
            editable: false,
            formatter: (v: unknown) =>
              v == null || v === '' ? '' : `${Number(v).toLocaleString()}円`,
            aggregation: 'sum',
            width: '140px',
          },
        ],
      }),
    ),
  )

  const pivotTableDef = computed<MultiLevelHeaderTableDef<ApplicationMediaPivotRow>>(() => ({
    row: {
      key: 'mediaName',
      title: '応募媒体名',
      filter: { type: 'text' },
    },
    columns: columns.value,
  }))

  return {
    pivotTableDef,
  }
}