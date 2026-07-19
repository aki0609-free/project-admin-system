import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type {
  MailRecipientForm,
  MailRecipientGroupForm,
} from '@/features/system/mail/types/mailFormTypes'

export const mailRecipientGroupFields: GridFormFieldDef<MailRecipientGroupForm>[] = [
  { key: 'id', label: 'ID', type: 'number', width: 100 },
  { key: 'groupKey', label: 'groupKey', type: 'text', gridColumn: '2 / span 3' },
  { key: 'groupName', label: 'groupName', type: 'text', gridColumn: '1 / span 4' },
  { key: 'activeFlag', label: '有効', type: 'checkbox', width: 100 },
]

export const mailRecipientFields: GridFormFieldDef<MailRecipientForm>[] = [
  {
    key: 'recipientType',
    label: '種別',
    type: 'select',
    gridColumn: '1 / span 2',
    options: [
      { title: 'TO', value: 'TO' },
      { title: 'CC', value: 'CC' },
      { title: 'BCC', value: 'BCC' },
    ],
  },
  { key: 'recipientKey', label: 'recipientKey', type: 'text', gridColumn: '1 / span 4' },
  { key: 'recipientName', label: 'recipientName', type: 'text', gridColumn: '1 / span 4' },
  { key: 'email', label: 'email', type: 'text', gridColumn: '1 / span 4' },
  { key: 'activeFlag', label: '有効', type: 'checkbox', width: 100 },
]