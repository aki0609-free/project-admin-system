import { computed, reactive, watch, type Ref } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type { MailRecipientGroupResponse } from '@/features/system/mail/types/mailApiTypes'
import type { MailRecipientGroupForm } from '@/features/system/mail/types/mailFormTypes'
import {
  createEmptyMailRecipientGroupForm,
  toMailRecipientGroupForm,
} from '@/features/system/mail/utils/mailRecipientGroupFormFactory'
import { useMailRecipientSelection } from '@/features/system/mail/composables/useMailRecipientSelection'
import { mailRecipientFields, mailRecipientGroupFields } from '../types/mailRecipientGroupFieldDefs'
import { mailRecipientGroupSchema, mailRecipientSchema } from '../types/mailRecipientGroupSchemas'

export const useMailRecipientGroupEditDialog = (
  visible: Ref<boolean>,
  group: Ref<MailRecipientGroupResponse | null>,
  emitSave: (form: MailRecipientGroupForm) => void,
  emitDelete: (form: MailRecipientGroupForm) => void,
) => {
  const formModel = reactive<MailRecipientGroupForm>(createEmptyMailRecipientGroupForm())

  const {
    selectedRecipient,
    syncSelection,
    selectRecipient,
    selectFirstRecipient,
    clearSelection,
    addRecipient,
    removeRecipient,
  } = useMailRecipientSelection(formModel.recipients)

  const isEdit = computed(() => formModel.id > 0)

  const resetForm = () => {
    const empty = createEmptyMailRecipientGroupForm()

    formModel.id = empty.id
    formModel.groupKey = empty.groupKey
    formModel.groupName = empty.groupName
    formModel.activeFlag = empty.activeFlag

    formModel.recipients.splice(0)

    clearSelection()
  }

  const applyGroup = (value: MailRecipientGroupResponse) => {
    const next = toMailRecipientGroupForm(value)

    formModel.id = next.id
    formModel.groupKey = next.groupKey
    formModel.groupName = next.groupName
    formModel.activeFlag = next.activeFlag

    formModel.recipients.splice(0, formModel.recipients.length, ...next.recipients)

    selectFirstRecipient()
  }

  watch(
    () => visible.value,
    (opened) => {
      if (!opened) return

      if (!group.value) {
        resetForm()
      }
    },
    { immediate: true },
  )

  watch(
    () => group.value,
    (value) => {
      if (!visible.value) return

      if (!value) {
        resetForm()
        return
      }

      applyGroup(value)
    },
    { immediate: true },
  )

  watch(
    () => formModel.recipients,
    () => syncSelection(),
    { deep: true },
  )

  const save = () => {
    emitSave({
      ...formModel,
      recipients: formModel.recipients.map((item) => ({ ...item })),
    })
  }

  const remove = () => {
    emitDelete({
      ...formModel,
      recipients: formModel.recipients.map((item) => ({ ...item })),
    })
  }

  const close = () => {
    visible.value = false
  }

  const footerItems = computed<ToolbarItem[]>(() => {
    const items: ToolbarItem[] = []

    if (isEdit.value) {
      items.push({
        type: 'button',
        label: '削除',
        color: 'error',
        onClick: remove,
      })
    }

    items.push(
      {
        type: 'button',
        label: '閉じる',
        color: 'secondary',
        onClick: close,
      },
      {
        type: 'button',
        label: '保存',
        color: 'primary',
        onClick: save,
      },
    )

    return items
  })

  const recipientToolbarItems = computed<ToolbarItem[]>(() => [
    { type: 'button', label: '追加', color: 'primary', onClick: addRecipient },
    {
      type: 'button',
      label: '削除',
      color: 'error',
      disabled: selectedRecipient.value == null,
      onClick: removeRecipient,
    },
  ])

  return {
    formModel,
    selectedRecipient,
    isEdit,
    fields: mailRecipientGroupFields,
    recipientFields: mailRecipientFields,
    schema: mailRecipientGroupSchema,
    recipientSchema: mailRecipientSchema,
    footerItems,
    recipientToolbarItems,
    selectRecipient,
  }
}
