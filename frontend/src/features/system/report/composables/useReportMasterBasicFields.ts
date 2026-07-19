import { computed, type Ref } from 'vue'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { ReportMasterForm } from '@/features/system/report/types/reportFormTypes'

export type SelectOption = {
  title: string
  value: string
}

export const useReportMasterBasicFields = (
  templateOptions: Ref<SelectOption[]>,
) => {
  const fields = computed<GridFormFieldDef<ReportMasterForm>[]>(() => [
    {
      key: 'reportCode',
      label: '帳票コード',
      type: 'text',
      gridColumn: '1 / span 2',
    },
    {
      key: 'reportName',
      label: '帳票名',
      type: 'text',
      gridColumn: '3 / span 2',
    },
    {
      key: 'templateFileName',
      label: 'テンプレート',
      type: 'select',
      gridColumn: '1 / span 2',
      options: templateOptions.value,
    },
    {
      key: 'outputFormat',
      label: '出力形式',
      type: 'select',
      gridColumn: '3 / span 1',
      options: [
        { title: 'PDF', value: 'PDF' },
        { title: 'CSV', value: 'CSV' },
        { title: 'EXCEL', value: 'EXCEL' },
      ],
    },
    {
      key: 'fileName',
      label: 'ファイル名',
      type: 'text',
      gridColumn: '4 / span 1',
    },
    {
      key: 'workTable',
      label: 'workTable',
      type: 'text',
      gridColumn: '1 / span 2',
    },
    {
      key: 'inputTable',
      label: 'inputTable',
      type: 'text',
      gridColumn: '3 / span 1',
    },
    {
      key: 'outputTable',
      label: 'outputTable',
      type: 'text',
      gridColumn: '4 / span 1',
    },
    {
      key: 'layoutType',
      label: 'レイアウト種別',
      type: 'select',
      gridColumn: '1 / span 1',
      options: [
        { title: 'SINGLE', value: 'SINGLE' },
        { title: 'MULTI', value: 'MULTI' },
      ],
    },
    {
      key: 'layoutCount',
      label: 'レイアウト数',
      type: 'number',
      gridColumn: '2 / span 1',
    },
    {
      key: 'previewEnabled',
      label: 'プレビュー',
      type: 'checkbox',
      width: 80,
    },
    {
      key: 'useSignature',
      label: '署名使用',
      type: 'checkbox',
      width: 80,
    },
    {
      key: 'activeFlag',
      label: '有効',
      type: 'checkbox',
      width: 80,
    },
  ])

  return {
    fields,
  }
}