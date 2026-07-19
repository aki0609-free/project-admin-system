import { computed, type Ref } from 'vue'
import type { AllowanceDetailViewType } from '@/features/master/allowance/types/allowanceTypes'
import { useGenericAllowanceDetailConfig } from '@/features/master/allowance/composables/details/useGenericAllowanceDetailConfig'

export const useAllowanceDetailConfig = (
  detailViewType: Ref<AllowanceDetailViewType>,
) => {
  const generic = useGenericAllowanceDetailConfig()

  const current = computed(() => {
    switch (detailViewType.value) {
      case 'NONE':
      default:
        return generic
    }
  })

  return {
    title: computed(() => current.value.title),
    description: computed(() => current.value.description),
    rows: computed(() => current.value.rows.value),
    columns: computed(() => current.value.columns.value),
    filterRules: computed(() => current.value.filterRules.value),
  }
}