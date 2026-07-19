import { computed } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { ReportMasterListItemResponse } from '@/features/system/report/types/reportMasterApiTypes'

export type ReportMasterTableRow = SimpleTableEditableRow & {
  id: number
  reportCode: string
  reportName: string
  templateFileName: string
  workTable: string
  outputFormat: string
  previewEnabledText: string
  useSignatureText: string
  activeText: string
  raw: ReportMasterListItemResponse
}

export const useReportMasterTableConfig = (
  reportMasters: Readonly<{
    value: ReportMasterListItemResponse[]
  }>,
) => {
  const rows = computed<ReportMasterTableRow[]>(() =>
    reportMasters.value.map((item) => ({
      id: item.id,
      reportCode: item.reportCode,
      reportName: item.reportName,
      templateFileName: item.templateFileName ?? '',
      workTable: item.workTable,
      outputFormat: item.outputFormat,
      previewEnabledText: item.previewEnabled ? 'あり' : 'なし',
      useSignatureText: item.useSignature ? 'あり' : 'なし',
      activeText: item.activeFlag ? '有効' : '無効',
      raw: item,
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<ReportMasterTableRow>[] = [
      { title: 'ID', key: 'id', width: '200px', filter: { type: 'text' } },
      { title: '帳票コード', key: 'reportCode', width: '200px', filter: { type: 'text' } },
      { title: '帳票名', key: 'reportName', width: '220px', filter: { type: 'text' } },
      { title: 'テンプレート', key: 'templateFileName', width: '220px', filter: { type: 'text' } },
      { title: 'workTable', key: 'workTable', width: '200px', filter: { type: 'text' } },
      { title: '出力形式', key: 'outputFormat', width: '200px', filter: { type: 'text' } },
      { title: 'プレビュー', key: 'previewEnabledText', width: '200px', filter: { type: 'text' } },
      { title: '署名', key: 'useSignatureText', width: '200px', filter: { type: 'text' } },
      { title: '状態', key: 'activeText', width: '200px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<ReportMasterTableRow>(columns.value),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}