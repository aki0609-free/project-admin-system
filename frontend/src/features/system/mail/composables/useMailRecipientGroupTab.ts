import type { ToolbarItem } from "@/toolbox/toolbar/types/types"
import { computed, ref } from "vue"
import { useCreateMailRecipientGroupMutation } from "../api/mutations/useCreateMailRecipientGroupMutation"
import { useDeleteMailRecipientGroupMutation } from "../api/mutations/useDeleteMailRecipientGroupMutation"
import { useUpdateMailRecipientGroupMutation } from "../api/mutations/useUpdateMailRecipientGroupMutation"
import { useMailRecipientGroupDetailQuery } from "../api/queries/useMailRecipientGroupDetailQuery"
import { useMailRecipientGroupsQuery } from "../api/queries/useMailRecipientGroupsQuery"
import type { MailRecipientGroupResponse } from "../types/mailApiTypes"
import type { MailRecipientGroupForm } from "../types/mailFormTypes"
import { toMailRecipientGroupSaveRequest } from "../utils/mailRecipientGroupConverters"

export const useMailRecipientGroupTab = () => {
  const groupsQuery = useMailRecipientGroupsQuery()
  const createMutation = useCreateMailRecipientGroupMutation()
  const updateMutation = useUpdateMailRecipientGroupMutation()
  const deleteMutation = useDeleteMailRecipientGroupMutation()

  const visible = ref(false)
  const editingId = ref<number | null>(null)

  const detailQuery = useMailRecipientGroupDetailQuery(
    computed(() => editingId.value),
  )

  const dialogGroup = computed(() =>
    visible.value ? detailQuery.group.value : null,
  )

  const busy = computed(
    () =>
      groupsQuery.isLoading.value ||
      detailQuery.isLoading.value ||
      createMutation.isPending.value ||
      updateMutation.isPending.value ||
      deleteMutation.isPending.value,
  )

  const openCreate = () => {
    editingId.value = null
    visible.value = true
  }

  const openEdit = (row: MailRecipientGroupResponse) => {
    editingId.value = row.id
    visible.value = true
  }

  const close = () => {
    visible.value = false
    editingId.value = null
  }

  const save = async (form: MailRecipientGroupForm) => {
    const request = toMailRecipientGroupSaveRequest(form)

    if (form.id > 0) {
      await updateMutation.mutateAsync({
        id: form.id,
        request,
      })
    } else {
      await createMutation.mutateAsync(request)
    }

    close()
  }

  const remove = async (form: MailRecipientGroupForm) => {
    if (form.id <= 0) {
      close()
      return
    }

    const ok = window.confirm(`「${form.groupKey}」を削除しますか？`)
    if (!ok) return

    await deleteMutation.mutateAsync({ id: form.id })
    close()
  }

  const toolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '新規追加',
      color: 'primary',
      disabled: busy.value,
      onClick: openCreate,
    },
  ])

  return {
    groupsQuery,
    visible,
    dialogGroup,
    toolbarItems,
    openEdit,
    save,
    remove,
  }
}