import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { ReportSignatureFormModel } from '@/features/system/report/types/reportSignatureFormTypes'

export const useReportSignatureFields = () => {
  const fields: GridFormFieldDef<ReportSignatureFormModel>[] = [
    {
      key: 'reportMasterId',
      label: 'MasterID',
      type: 'number',
      width: 120,
    },
    {
      key: 'signatureName',
      label: '署名名',
      type: 'text',
      gridColumn: '2 / span 2',
    },
    {
      key: 'signatureType',
      label: '種別',
      type: 'select',
      options: [
        { title: 'STAMP', value: 'STAMP' },
        { title: 'HANDWRITTEN', value: 'HANDWRITTEN' },
        { title: 'NAME_SEAL', value: 'NAME_SEAL' },
      ],
    },
    {
      key: 'fileName',
      label: 'ファイル名',
      type: 'text',
      gridColumn: '1 / span 2',
    },
    {
      key: 'contentType',
      label: 'ContentType',
      type: 'text',
      gridColumn: '3 / span 2',
    },
    {
      key: 'width',
      label: '幅',
      type: 'number',
    },
    {
      key: 'height',
      label: '高さ',
      type: 'number',
    },
    {
      key: 'displayOrder',
      label: '表示順',
      type: 'number',
    },
    {
      key: 'defaultFlag',
      label: '既定',
      type: 'checkbox',
      width: 100,
    },
    {
      key: 'activeFlag',
      label: '有効',
      type: 'checkbox',
      width: 100,
    },
  ]

  return {
    fields,
  }
}