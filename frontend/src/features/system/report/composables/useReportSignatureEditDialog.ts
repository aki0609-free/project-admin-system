import { computed, reactive, ref, watch, type Ref } from 'vue'
import { z } from 'zod'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type { ReportSignatureResponse } from '@/features/system/report/types/reportSignatureApiTypes'
import type { ReportSignatureFormModel } from '@/features/system/report/types/reportSignatureFormTypes'
import { useCreateReportSignatureMutation } from '@/features/system/report/api/mutations/useCreateReportSignatureMutation'
import { useUpdateReportSignatureMutation } from '@/features/system/report/api/mutations/useUpdateReportSignatureMutation'
import { useDeleteReportSignatureMutation } from '@/features/system/report/api/mutations/useDeleteReportSignatureMutation'
import { useReportSignatureFields } from '@/features/system/report/composables/useReportSignatureFields'
import { useReportSignatureFileUpload } from '@/features/system/report/composables/useReportSignatureFileUpload'
import {
  applyReportSignatureDetailToForm,
  createEmptyReportSignatureForm,
  toReportSignatureSaveRequest,
} from '@/features/system/report/utils/reportSignatureFormFactory'

export const reportSignatureSchema = z.object({
  reportMasterId: z.number().min(1),
  signatureName: z.string().min(1, '必須です'),
  signatureType: z.enum(['STAMP', 'HANDWRITTEN', 'NAME_SEAL']),
  fileName: z.string(),
  contentType: z.string(),
  width: z.number().min(1),
  height: z.number().min(1),
  displayOrder: z.number().min(1),
  defaultFlag: z.boolean(),
  activeFlag: z.boolean(),
  signatureImageBase64: z.string(),
})

export const useReportSignatureEditDialog = (
  visible: Ref<boolean>,
  reportSignatureId: Ref<number | null>,
  detail: Ref<ReportSignatureResponse | null>,
  close: () => void,
) => {
  const createMutation = useCreateReportSignatureMutation()
  const updateMutation = useUpdateReportSignatureMutation()
  const deleteMutation = useDeleteReportSignatureMutation()

  const formModel = reactive<ReportSignatureFormModel>(
    createEmptyReportSignatureForm(),
  )

  const previewSrc = ref('')

  const isEdit = computed(() => reportSignatureId.value != null)

  const { fields } = useReportSignatureFields()

  const { onFileChange } = useReportSignatureFileUpload(
    formModel,
    previewSrc,
  )

  const busy = computed(
    () =>
      createMutation.isPending.value ||
      updateMutation.isPending.value ||
      deleteMutation.isPending.value,
  )

  const resetForm = () => {
    Object.assign(
      formModel,
      createEmptyReportSignatureForm(),
    )

    previewSrc.value = ''
  }

  const applyDetail = (
    item: ReportSignatureResponse,
  ) => {
    applyReportSignatureDetailToForm(
      formModel,
      item,
    )

    previewSrc.value = item.signatureImageBase64 ?? ''
  }

  watch(
    () => visible.value,
    (opened) => {
      if (!opened) return

      if (reportSignatureId.value == null) {
        resetForm()
      }
    },
    { immediate: true },
  )

  watch(
    () => detail.value,
    (item) => {
      if (!visible.value) return
      if (reportSignatureId.value == null) return
      if (!item) return

      applyDetail(item)
    },
    { immediate: true },
  )

  const save = async () => {
    const request = toReportSignatureSaveRequest(formModel)

    if (reportSignatureId.value == null) {
      await createMutation.mutateAsync(request)
    } else {
      await updateMutation.mutateAsync({
        id: reportSignatureId.value,
        request,
      })
    }

    close()
  }

  const remove = async () => {
    if (reportSignatureId.value == null) return

    const ok = window.confirm(
      `「${formModel.signatureName}」を削除しますか？`,
    )

    if (!ok) return

    await deleteMutation.mutateAsync(reportSignatureId.value)

    close()
  }

  const leftItems = computed<ToolbarItem[]>(() => {
    const items: ToolbarItem[] = []

    if (isEdit.value) {
      items.push({
        type: 'button',
        label: '削除',
        color: 'error',
        disabled: busy.value,
        onClick: remove,
      })
    }

    items.push({
      type: 'button',
      label: busy.value ? '保存中...' : '保存',
      color: 'primary',
      disabled: busy.value,
      onClick: save,
    })

    return items
  })

  const rightItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '閉じる',
      color: 'secondary',
      disabled: busy.value,
      onClick: close,
    },
  ])

  return {
    formModel,
    previewSrc,
    isEdit,
    fields,
    schema: reportSignatureSchema,
    busy,
    leftItems,
    rightItems,
    onFileChange,
  }
}