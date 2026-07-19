import { computed } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import { useMailSendQueuesQuery } from '@/features/system/mail/api/queries/useMailSendQueuesQuery'
import { useSendWaitingMailsMutation } from '@/features/system/mail/api/mutations/useSendWaitingMailsMutation'
import { useRetryMailQueueMutation } from '@/features/system/mail/api/mutations/useRetryMailQueueMutation'
import type {
  MailSendQueueResponse,
  MailSendResult,
} from '@/features/system/mail/types/mailApiTypes'

export type MailQueueTableRow = SimpleTableEditableRow & {
  id: number
  mailType: string
  businessKey: string
  toAddressesText: string
  subject: string
  status: string
  retryText: string
  sentAt: string
  errorMessage: string
  raw: MailSendQueueResponse
}

export const useMailQueueTab = () => {
  const queuesQuery = useMailSendQueuesQuery()
  const sendWaitingMutation = useSendWaitingMailsMutation()
  const retryMutation = useRetryMailQueueMutation()

  const rows = computed<MailQueueTableRow[]>(() =>
    queuesQuery.queues.value.map(item => ({
      id: item.id,
      mailType: item.mailType ?? '',
      businessKey: item.businessKey ?? '',
      toAddressesText: item.toAddresses.join(', '),
      subject: item.subject,
      status: item.status,
      retryText: `${item.retryCount} / ${item.maxRetryCount}`,
      sentAt: item.sentAt ?? '',
      errorMessage: item.lastErrorMessage ?? '',
      raw: item,
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<MailQueueTableRow>[] = [
      { title: 'ID', key: 'id', width: '80px', filter: { type: 'text' } },
      { title: 'mailType', key: 'mailType', width: '160px', filter: { type: 'text' } },
      { title: 'businessKey', key: 'businessKey', width: '220px', filter: { type: 'text' } },
      { title: '宛先', key: 'toAddressesText', width: '260px', filter: { type: 'text' } },
      { title: '件名', key: 'subject', width: '280px', filter: { type: 'text' } },
      { title: '状態', key: 'status', width: '120px', filter: { type: 'text' } },
      { title: 'Retry', key: 'retryText', width: '120px', filter: { type: 'text' } },
      { title: '送信日時', key: 'sentAt', width: '250px', filter: { type: 'text' } },
      { title: 'エラー', key: 'errorMessage', width: '320px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<MailQueueTableRow>(columns.value),
  )

  const sendWaiting = async () => {
    const result = await sendWaitingMutation.mutateAsync(undefined) as MailSendResult
    alert(result.message)
  }

  const retry = async (row: MailQueueTableRow) => {
    await retryMutation.mutateAsync(row.id)
    alert('再送対象に戻しました。')
  }

  const toolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: sendWaitingMutation.isPending.value ? '送信中...' : 'WAITING送信',
      color: 'primary',
      disabled: sendWaitingMutation.isPending.value,
      onClick: sendWaiting,
    },
  ])

  return {
    rows,
    columns,
    filterRules,
    toolbarItems,
    retryMutation,
    retry,
  }
}