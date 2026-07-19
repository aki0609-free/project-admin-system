import { computed } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { ReportSignatureResponse } from '@/features/system/report/types/reportSignatureApiTypes'

export type ReportSignatureTableRow =
  SimpleTableEditableRow & {
    id: number
    reportMasterId: number
    signatureName: string
    signatureType: string
    fileName: string
    width: number | null
    height: number | null
    defaultFlag: boolean
    activeFlag: boolean
    raw: ReportSignatureResponse
  }

export const useReportSignatureTableConfig = (
  reportSignatures: Readonly<{
    value: ReportSignatureResponse[]
  }>,
) => {
  const rows = computed<ReportSignatureTableRow[]>(() =>
    reportSignatures.value.map((item) => ({
      id: item.id,
      reportMasterId: item.reportMasterId,
      signatureName: item.signatureName,
      signatureType: item.signatureType,
      fileName: item.fileName ?? '',
      width: item.width ?? null,
      height: item.height ?? null,
      defaultFlag: item.defaultFlag,
      activeFlag: item.activeFlag,
      raw: item,
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<ReportSignatureTableRow>[] = [
      {
        title: 'ID',
        key: 'id',
        width: '200px',
        filter: { type: 'text' },
      },
      {
        title: 'MasterID',
        key: 'reportMasterId',
        width: '200px',
        filter: { type: 'text' },
      },
      {
        title: '署名名',
        key: 'signatureName',
        width: '300px',
        filter: { type: 'text' },
      },
      {
        title: '種別',
        key: 'signatureType',
        width: '200px',
        filter: { type: 'text' },
      },
      {
        title: 'ファイル名',
        key: 'fileName',
        width: '300px',
        filter: { type: 'text' },
      },
      {
        title: 'サイズ',
        key: 'width',
        width: '200px',
        filter: { type: 'text' },
      },
      {
        title: '既定',
        key: 'defaultFlag',
        width: '200px',
        filter: { type: 'text' },
      },
      {
        title: '状態',
        key: 'activeFlag',
        width: '200px',
        filter: { type: 'text' },
      },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<ReportSignatureTableRow>(
      columns.value,
    ),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}