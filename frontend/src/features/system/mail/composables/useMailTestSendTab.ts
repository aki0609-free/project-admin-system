import { computed, reactive, ref } from 'vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import { useSendTestMailMutation } from '@/features/system/mail/api/mutations/useSendTestMailMutation'
import { toMailTestSendRequest } from '@/features/system/mail/utils/mailTestSendConverters'
import type { MailSendResult } from '@/features/system/mail/types/mailApiTypes'
import type { MailTestSendForm } from '@/features/system/mail/types/mailFormTypes'
import { createMailTestSendForm } from '../utils/mailRecipientGroupFormFactory'
import { mailTestSendFields } from '../types/mailTestSendFieldDefs'
import { mailTestSendSchema } from '../types/mailTestSendSchemas'

export const useMailTestSendTab = () => {
  const sendMutation = useSendTestMailMutation()
  const lastMessage = ref('')

  const formModel = reactive<MailTestSendForm>(
    createMailTestSendForm(),
  )

  const send = async () => {
    lastMessage.value = ''

    const result = await sendMutation.mutateAsync(
      toMailTestSendRequest(formModel),
    ) as MailSendResult

    lastMessage.value = result.message
    alert(result.message)
  }

  const leftToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: sendMutation.isPending.value ? '送信中...' : 'テスト送信',
      color: 'primary',
      intent: 'primary',
      loading: sendMutation.isPending.value,
      disabled: sendMutation.isPending.value,
      onClick: send,
    },
  ])

  return {
    formModel,
    fields: mailTestSendFields,
    schema: mailTestSendSchema,
    sendMutation,
    lastMessage,
    send,
    leftToolbarItems,
  }
}
