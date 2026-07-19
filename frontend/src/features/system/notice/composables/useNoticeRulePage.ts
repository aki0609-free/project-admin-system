import { computed, ref } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useNoticeRulesQuery } from '@/features/system/notice/api/queries/useNoticeRulesQuery'
import { useNoticeRuleDetailQuery } from '@/features/system/notice/api/queries/useNoticeRuleDetailQuery'
import { useCreateNoticeRuleMutation } from '@/features/system/notice/api/mutations/useCreateNoticeRuleMutation'
import { useUpdateNoticeRuleMutation } from '@/features/system/notice/api/mutations/useUpdateNoticeRuleMutation'
import { useDeleteNoticeRuleMutation } from '@/features/system/notice/api/mutations/useDeleteNoticeRuleMutation'
import { useGenerateNoticeRulesMutation } from '@/features/system/notice/api/mutations/useGenerateNoticeRulesMutation'
import { useReloadNoticeRuleSchedulesMutation } from '@/features/system/notice/api/mutations/useReloadNoticeRuleSchedulesMutation'
import { toNoticeRuleSaveRequest } from '@/features/system/notice/utils/noticeRuleConverters'
import type { NoticeRuleForm } from '@/features/system/notice/types/noticeRuleFormTypes'
import type { NoticeRuleTableRow } from '@/features/system/notice/composables/useNoticeRuleTableConfig'

export const useNoticeRulePage = () => {
  const rulesQuery = useNoticeRulesQuery()
  const createMutation = useCreateNoticeRuleMutation()
  const updateMutation = useUpdateNoticeRuleMutation()
  const deleteMutation = useDeleteNoticeRuleMutation()
  const generateMutation = useGenerateNoticeRulesMutation()
  const reloadSchedulesMutation = useReloadNoticeRuleSchedulesMutation()

  const dialogVisible = ref(false)
  const editingId = ref<number | null>(null)

  const detailQuery = useNoticeRuleDetailQuery(
    computed(() => editingId.value),
  )

  const dialogRule = computed(() =>
    editingId.value == null ? null : detailQuery.rule.value,
  )

  const busy = computed(
    () =>
      rulesQuery.isLoading.value ||
      detailQuery.isLoading.value ||
      createMutation.isPending.value ||
      updateMutation.isPending.value ||
      deleteMutation.isPending.value ||
      generateMutation.isPending.value ||
      reloadSchedulesMutation.isPending.value,
  )

  const openCreate = () => {
    editingId.value = null
    dialogVisible.value = true
  }

  const openEdit = (row: NoticeRuleTableRow) => {
    editingId.value = row.id
    dialogVisible.value = true
  }

  const closeDialog = () => {
    dialogVisible.value = false
  }

  const save = async (form: NoticeRuleForm) => {
    const request = toNoticeRuleSaveRequest(form)

    if (form.id > 0) {
      await updateMutation.mutateAsync({
        id: form.id,
        request,
      })
    } else {
      await createMutation.mutateAsync(request)
    }

    closeDialog()
  }

  const remove = async (form: NoticeRuleForm) => {
    if (form.id <= 0) {
      closeDialog()
      return
    }

    const ok = window.confirm(`「${form.ruleName}」を削除しますか？`)
    if (!ok) return

    await deleteMutation.mutateAsync(form.id)
    closeDialog()
  }

  const generateAll = async () => {
    const result = await generateMutation.mutateAsync(undefined) as any
    alert(result.message)
  }

  const reloadSchedules = async () => {
    await reloadSchedulesMutation.mutateAsync(undefined)
    alert('NoticeRuleスケジュールを再読込しました。')
  }

  const toolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '新規追加',
      color: 'primary',
      disabled: busy.value,
      onClick: openCreate,
    },
    {
      type: 'button',
      label: '手動生成',
      color: 'secondary',
      disabled: busy.value,
      onClick: generateAll,
    },
    {
      type: 'button',
      label: 'スケジュール再読込',
      color: 'secondary',
      disabled: busy.value,
      onClick: reloadSchedules,
    },
  ])

  return {
    rulesQuery,
    dialogVisible,
    dialogRule,
    toolbarItems,
    openEdit,
    save,
    remove,
  }
}