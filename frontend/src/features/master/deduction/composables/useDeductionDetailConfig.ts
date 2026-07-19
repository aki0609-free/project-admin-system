import { computed, type Ref } from 'vue'
import type { DeductionDetailViewType } from '@/features/master/deduction/types/deductionTypes'
import { useIncomeTaxDetailConfig } from '@/features/master/deduction/composables/details/useIncomeTaxDetailConfig'
import { useResidentTaxDetailConfig } from '@/features/master/deduction/composables/details/useResidentTaxDetailConfig'
import { useGenericDeductionDetailConfig } from '@/features/master/deduction/composables/details/useGenericDeductionDetailConfig'
import { useInsuranceRateDetailConfig } from './details/useInsuranceRateDetailConfig'
import { useStandardSalaryDetailConfig } from './details/useStandardSalaryDetailConfig'

export const useDeductionDetailConfig = (
  detailViewType: Ref<DeductionDetailViewType>,
) => {
  const incomeTax = useIncomeTaxDetailConfig()
  const residentTax = useResidentTaxDetailConfig()
  const generic = useGenericDeductionDetailConfig()
  const insuranceRate = useInsuranceRateDetailConfig()
  const standardSalary = useStandardSalaryDetailConfig()

  const current = computed(() => {
    switch (detailViewType.value) {
      case 'INCOME_TAX':
        return incomeTax

      case 'RESIDENT_TAX':
        return residentTax

      case 'HEALTH_INSURANCE':
      case 'EMPLOYMENT_INSURANCE':
        return insuranceRate

      case 'PENSION':
        return standardSalary

      case 'GENERIC':
      case 'ADVANCE_PAYMENT':
      case 'DORMITORY':
      case 'MOBILE_PHONE':
      case 'WIFI':
      case 'SAVINGS':
      case 'LOAN_REPAYMENT':
      case 'LEGAL_DEPOSIT':
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