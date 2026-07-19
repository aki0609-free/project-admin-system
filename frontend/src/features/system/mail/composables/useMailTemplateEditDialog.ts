import { computed, reactive, ref, watch, type Ref } from 'vue'
import type { ToolbarItem } from '@/toolbox/toolbar/types/types'
import type {
  MailTemplatePreviewResponse,
  MailTemplateResponse,
} from '@/features/system/mail/types/mailApiTypes'
import type { MailTemplateForm } from '@/features/system/mail/types/mailFormTypes'
import { usePreviewMailTemplateMutation } from '@/features/system/mail/api/mutations/usePreviewMailTemplateMutation'
import {
  createEmptyMailTemplateForm,
  toMailTemplateForm,
} from '@/features/system/mail/utils/mailTemplateConverters'

const previewVariables = {
  executionId: 'preview-execution-id',
  reportCode: 'MONTHLY_PAY_SLIP',
  businessKey: 'MONTHLY_PAY_SLIP:2026-07:1',
  recipientKey: '1',
  recipientName: '山田太郎',
  recipientEmail: 'preview@example.com',
  fileName: '給与明細_2026-07.pdf',
  mailType: 'MONTHLY_PAY_SLIP',
}

export const useMailTemplateEditDialog = (
  visible: Ref<boolean>,
  template: Ref<MailTemplateResponse | null>,
  emitSave: (form: MailTemplateForm) => void,
  emitDelete: (form: MailTemplateForm) => void,
) => {
  const formModel = reactive<MailTemplateForm>(createEmptyMailTemplateForm())
  const previewMutation = usePreviewMailTemplateMutation()
  const previewResult = ref<MailTemplatePreviewResponse | null>(null)
  const editorRevision = ref(0)
  const isEdit = computed(() => formModel.id > 0)
  const canSave = computed(
    () =>
      formModel.templateKey.trim().length > 0 &&
      formModel.templateName.trim().length > 0 &&
      formModel.subjectTemplate.trim().length > 0 &&
      formModel.bodyTemplate.trim().length > 0,
  )

  const applyForm = (next: MailTemplateForm) => {
    Object.assign(formModel, next)
    previewResult.value = null
    editorRevision.value++
  }

  watch(
    () => visible.value,
    opened => {
      if (opened && !template.value) {
        applyForm(createEmptyMailTemplateForm())
      }
    },
    { immediate: true },
  )

  watch(
    () => template.value,
    value => {
      if (!visible.value || !value) return
      applyForm(toMailTemplateForm(value))
    },
    { immediate: true },
  )

  const preview = async () => {
    previewResult.value = await previewMutation.mutateAsync({
      subjectTemplate: formModel.subjectTemplate,
      bodyTemplate: formModel.bodyTemplate,
      htmlFlag: formModel.htmlFlag,
      variables: previewVariables,
    }) as MailTemplatePreviewResponse
  }

  const snapshot = (): MailTemplateForm => ({ ...formModel })

  const footerItems = computed<ToolbarItem[]>(() => {
    const items: ToolbarItem[] = []

    if (isEdit.value) {
      items.push({
        type: 'button',
        label: '削除',
        color: 'error',
        onClick: () => emitDelete(snapshot()),
      })
    }

    items.push(
      {
        type: 'button',
        label: 'プレビュー',
        color: 'secondary',
        disabled: !canSave.value || previewMutation.isPending.value,
        onClick: preview,
      },
      {
        type: 'button',
        label: '閉じる',
        color: 'secondary',
        onClick: () => { visible.value = false },
      },
      {
        type: 'button',
        label: '保存',
        color: 'primary',
        disabled: !canSave.value,
        onClick: () => emitSave(snapshot()),
      },
    )

    return items
  })

  return {
    formModel,
    previewResult,
    editorRevision,
    isEdit,
    footerItems,
  }
}
