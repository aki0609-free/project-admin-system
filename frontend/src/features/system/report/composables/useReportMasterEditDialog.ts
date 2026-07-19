import { computed, ref, watch, type Ref } from 'vue'
import { z } from 'zod'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useReportMasterDetailQuery } from '@/features/system/report/api/queries/useReportMasterDetailQuery'
import { useCreateReportMasterMutation } from '@/features/system/report/api/mutations/useCreateReportMasterMutation'
import { useUpdateReportMasterMutation } from '@/features/system/report/api/mutations/useUpdateReportMasterMutation'
import { useDeleteReportMasterMutation } from '@/features/system/report/api/mutations/useDeleteReportMasterMutation'
import { useReportTemplatesQuery } from '@/features/system/report/api/queries/useReportTemplatesQuery'
import type { ReportMasterForm } from '@/features/system/report/types/reportFormTypes'
import {
  createEmptyReportMasterForm,
  toReportMasterForm,
  toReportMasterSaveRequest,
} from '@/features/system/report/utils/reportFormFactory'

export const reportMasterSchema = z.object({
  reportCode: z.string().min(1, '必須です'),
  reportName: z.string().min(1, '必須です'),
  templateFileName: z.string().nullable(),
  workTable: z.string().min(1, '必須です'),
  inputTable: z.string().nullable(),
  outputTable: z.string().nullable(),
  preProcessType: z.enum(['NONE', 'SQL', 'PROCEDURE']),
  preProcessSql: z.string().nullable(),
  procedureName: z.string().nullable(),
  querySql: z.string().nullable(),
  cleanupType: z.enum(['NONE', 'SQL', 'PROCEDURE']),
  cleanupSql: z.string().nullable(),
  cleanupProcedureName: z.string().nullable(),
  layoutType: z.enum(['SINGLE', 'MULTI']),
  layoutCount: z.number().nullable(),
  fileName: z.string().nullable(),
  outputFormat: z.enum(['PDF', 'CSV', 'EXCEL']),
  useSignature: z.boolean(),
  previewEnabled: z.boolean(),
  activeFlag: z.boolean(),
  params: z.array(z.any()),
})

export const useReportMasterEditDialog = (
  visible: Ref<boolean>,
  reportMasterId: Ref<number | null>,
  close: () => void,
) => {
  const activeTab = ref<'basic' | 'execution' | 'params'>('basic')
  const form = ref<ReportMasterForm>(createEmptyReportMasterForm())

  const detailQuery = useReportMasterDetailQuery(reportMasterId)
  const templatesQuery = useReportTemplatesQuery()
  const createMutation = useCreateReportMasterMutation()
  const updateMutation = useUpdateReportMasterMutation()
  const deleteMutation = useDeleteReportMasterMutation()

  const tabs = [
    { label: '基本情報', value: 'basic' },
    { label: '実行設定', value: 'execution' },
    { label: 'パラメータ', value: 'params' },
  ]

  const templateOptions = computed(() =>
    templatesQuery.templates.value.map((item) => ({
      title: item.fileName,
      value: item.fileName,
    })),
  )

  const isEdit = computed(() => reportMasterId.value != null)

  const busy = computed(
    () =>
      detailQuery.isLoading.value ||
      templatesQuery.isLoading.value ||
      createMutation.isPending.value ||
      updateMutation.isPending.value ||
      deleteMutation.isPending.value,
  )

  const resetForm = () => {
    form.value = createEmptyReportMasterForm()
    activeTab.value = 'basic'
  }

  watch(
    visible,
    (opened) => {
      if (!opened) return

      if (reportMasterId.value == null) {
        resetForm()
      }
    },
    { immediate: true },
  )

  watch(
    () => detailQuery.reportMaster.value,
    (detail) => {
      if (!visible.value) return
      if (reportMasterId.value == null) return
      if (!detail) return

      form.value = toReportMasterForm(detail)
      activeTab.value = 'basic'
    },
    { immediate: true },
  )

  const save = async () => {
    const request = toReportMasterSaveRequest(form.value)

    if (reportMasterId.value == null) {
      await createMutation.mutateAsync(request)
    } else {
      await updateMutation.mutateAsync({
        id: reportMasterId.value,
        request,
      })
    }

    close()
  }

  const remove = async () => {
    const id = reportMasterId.value

    if (typeof id !== 'number') {
      alert('削除対象のIDが不正です。')
      return
    }

    const ok = window.confirm('この帳票定義を削除しますか？')
    if (!ok) return

    await deleteMutation.mutateAsync({ id })
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
    activeTab,
    form,
    tabs,
    schema: reportMasterSchema,
    templateOptions,
    isEdit,
    busy,
    leftItems,
    rightItems,
    save,
    remove,
  }
}