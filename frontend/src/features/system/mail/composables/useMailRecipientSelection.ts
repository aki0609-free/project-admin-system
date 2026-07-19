import { computed, ref, type Reactive } from 'vue'
import type { MailRecipientForm } from '@/features/system/mail/types/mailFormTypes'
import { createEmptyMailRecipient } from '@/features/system/mail/utils/mailRecipientGroupFormFactory'

export const useMailRecipientSelection = (
  recipients: Reactive<MailRecipientForm[]>,
) => {
  const selectedRecipientId = ref<number | null>(null)

  const selectedRecipientIndex = computed(() => {
    if (selectedRecipientId.value == null) return -1
    return recipients.findIndex(item => item.id === selectedRecipientId.value)
  })

  const selectedRecipient = computed<MailRecipientForm | null>({
    get: () => {
      const index = selectedRecipientIndex.value
      if (index < 0) return null
      return recipients[index] ?? null
    },
    set: value => {
      const index = selectedRecipientIndex.value
      if (index < 0 || !value) return

      recipients.splice(index, 1, value)
    },
  })

  const syncSelection = () => {
    if (!recipients.length) {
      selectedRecipientId.value = null
      return
    }

    const exists = recipients.some(item => item.id === selectedRecipientId.value)

    if (!exists) {
      selectedRecipientId.value = recipients[0]?.id ?? null
    }
  }

  const selectRecipient = (id: number) => {
    selectedRecipientId.value = id
  }

  const selectFirstRecipient = () => {
    selectedRecipientId.value = recipients[0]?.id ?? null
  }

  const clearSelection = () => {
    selectedRecipientId.value = null
  }

  const addRecipient = () => {
    const next = createEmptyMailRecipient()
    recipients.push(next)
    selectedRecipientId.value = next.id
  }

  const removeRecipient = () => {
    const index = selectedRecipientIndex.value
    if (index < 0) return

    recipients.splice(index, 1)
    syncSelection()
  }

  return {
    selectedRecipientId,
    selectedRecipient,
    syncSelection,
    selectRecipient,
    selectFirstRecipient,
    clearSelection,
    addRecipient,
    removeRecipient,
  }
}