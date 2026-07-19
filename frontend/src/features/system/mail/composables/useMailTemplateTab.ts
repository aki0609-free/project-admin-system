import { computed, ref } from 'vue'
import type { ToolbarItem } from '@/toolbox/toolbar/types/types'
import { useMailTemplatesQuery } from '@/features/system/mail/api/queries/useMailTemplatesQuery'
import { useMailTemplateDetailQuery } from '@/features/system/mail/api/queries/useMailTemplateDetailQuery'
import { useCreateMailTemplateMutation } from '@/features/system/mail/api/mutations/useCreateMailTemplateMutation'
import { useUpdateMailTemplateMutation } from '@/features/system/mail/api/mutations/useUpdateMailTemplateMutation'
import { useDeleteMailTemplateMutation } from '@/features/system/mail/api/mutations/useDeleteMailTemplateMutation'
import type { MailTemplateResponse } from '@/features/system/mail/types/mailApiTypes'
import type { MailTemplateForm } from '@/features/system/mail/types/mailFormTypes'
import { toMailTemplateSaveRequest } from '@/features/system/mail/utils/mailTemplateConverters'

export const useMailTemplateTab = () => {
  const templatesQuery = useMailTemplatesQuery()
  const createMutation = useCreateMailTemplateMutation()
  const updateMutation = useUpdateMailTemplateMutation()
  const deleteMutation = useDeleteMailTemplateMutation()
  const visible = ref(false)
  const editingId = ref<number | null>(null)
  const detailQuery = useMailTemplateDetailQuery(
    computed(() => editingId.value),
  )

  const dialogTemplate = computed(() =>
    visible.value ? detailQuery.template.value : null,
  )

  const busy = computed(
    () =>
      templatesQuery.isLoading.value ||
      detailQuery.isLoading.value ||
      createMutation.isPending.value ||
      updateMutation.isPending.value ||
      deleteMutation.isPending.value,
  )

  const openCreate = () => {
    editingId.value = null
    visible.value = true
  }

  const openEdit = (template: MailTemplateResponse) => {
    editingId.value = template.id
    visible.value = true
  }

  const close = () => {
    visible.value = false
    editingId.value = null
  }

  const save = async (form: MailTemplateForm) => {
    const request = toMailTemplateSaveRequest(form)

    if (form.id > 0) {
      await updateMutation.mutateAsync({ id: form.id, request })
    } else {
      await createMutation.mutateAsync(request)
    }

    close()
  }

  const remove = async (form: MailTemplateForm) => {
    if (form.id <= 0) {
      close()
      return
    }

    if (!window.confirm(`「${form.templateKey}」を削除しますか？`)) return

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
    templatesQuery,
    visible,
    dialogTemplate,
    toolbarItems,
    openEdit,
    save,
    remove,
  }
}
