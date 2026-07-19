import { computed, ref } from 'vue'

import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import {
  useCreateRuleMutation,
  useDeleteRuleMutation,
  useUpdateRuleMutation,
} from '@/features/system/rule/api/useRuleMutations'
import { useRulesQuery } from '@/features/system/rule/api/useRulesQuery'
import type { RuleMasterResponse } from '@/features/system/rule/types/ruleApiTypes'
import type { RuleMasterForm } from '@/features/system/rule/types/ruleFormTypes'
import { toRuleMasterSaveRequest } from '../mapper/ruleFormMapper'


export const useRulePage = () => {
  const rulesQuery = useRulesQuery()

  const dialogVisible = ref(false)
  const dialogRule = ref<RuleMasterResponse | null>(null)

  const createMutation = useCreateRuleMutation()
  const updateMutation = useUpdateRuleMutation()
  const deleteMutation = useDeleteRuleMutation()

  const openCreate = () => {
    dialogRule.value = null
    dialogVisible.value = true
  }

  const openEdit = (rule: RuleMasterResponse) => {
    dialogRule.value = rule
    dialogVisible.value = true
  }

  const save = async (form: RuleMasterForm) => {
    const request = toRuleMasterSaveRequest(form)

    if (form.id > 0) {
      await updateMutation.mutateAsync({
        id: form.id,
        body: request,
      })
    } else {
      await createMutation.mutateAsync(request)
    }

    await rulesQuery.refetch()
    dialogVisible.value = false
    dialogRule.value = null
  }

  const remove = async (form: RuleMasterForm) => {
    if (form.id <= 0) return

    await deleteMutation.mutateAsync(form.id)
    await rulesQuery.refetch()

    dialogVisible.value = false
    dialogRule.value = null
  }

  const leftToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '新規作成',
      color: 'primary',
      icon: 'mdi-plus',
      onClick: openCreate,
    },
  ])

  return {
    rulesQuery,
    dialogVisible,
    dialogRule,
    leftToolbarItems,
    openCreate,
    openEdit,
    save,
    remove,
  }
}