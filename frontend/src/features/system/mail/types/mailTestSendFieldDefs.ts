import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { MailTestSendForm } from '@/features/system/mail/types/mailFormTypes'

export const mailTestSendFields: GridFormFieldDef<MailTestSendForm>[] = [
  { key: 'to', label: 'To', type: 'text', gridColumn: '1 / span 2' },
  { key: 'toName', label: '宛先名', type: 'text', gridColumn: '3 / span 2' },
  { key: 'cc', label: 'Cc', type: 'text', gridColumn: '1 / span 2' },
  { key: 'bcc', label: 'Bcc', type: 'text', gridColumn: '3 / span 2' },
  { key: 'subject', label: '件名', type: 'text', gridColumn: '1 / span 4' },
  {
    key: 'body',
    label: '本文',
    type: 'textarea',
    gridColumn: '1 / span 4',
    rows: 10,
    autoGrow: true,
  },
  {
    key: 'attachmentFilePath',
    label: '添付ファイルキー',
    type: 'text',
    gridColumn: '1 / span 3',
  },
  {
    key: 'attachmentFileName',
    label: '添付ファイル名',
    type: 'text',
    gridColumn: '4 / span 1',
  },
]