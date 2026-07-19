import { computed, reactive, type Ref } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import {
  applicantContractTypeLabelMap,
  applicantGenderLabelMap,
  applicantRetirementStatusLabelMap,
} from '@/features/application/constants/applicantConstants'
import { formatYearMonth } from '@/shared/utils/DateUtils'

export const useApplicantChartFilter = (
  applicants: Ref<ApplicantRow[]>,
) => {
  const filter = reactive({
    contactYearMonth: '',
    mediaName: '',
    retirementStatus: '',
    gender: '',
    contractType: '',
  })

  const yearMonthOptions = computed(() =>
    Array.from(
      new Set(
        applicants.value
          .map(item => item.contactDate?.slice(0, 7))
          .filter((v): v is string => !!v),
      ),
    )
      .sort()
      .map(value => ({
        label: formatYearMonth(value),
        value,
      })),
  )

  const mediaOptions = computed(() =>
    Array.from(
      new Set(
        applicants.value
          .map(item => item.mediaName)
          .filter((v): v is string => !!v),
      ),
    )
      .sort()
      .map(value => ({
        label: value,
        value,
      })),
  )

  const statusOptions = computed(() =>
    Array.from(
      new Set(
        applicants.value
          .map(item => item.retirementStatus)
          .filter((v) => !!v),
      ),
    ).map(value => ({
      label:
        applicantRetirementStatusLabelMap[
          value as keyof typeof applicantRetirementStatusLabelMap
        ] ?? value,
      value,
    })),
  )

  const genderOptions = computed(() =>
    Array.from(
      new Set(
        applicants.value
          .map(item => item.gender)
          .filter((v) => !!v),
      ),
    ).map(value => ({
      label:
        applicantGenderLabelMap[
          value as keyof typeof applicantGenderLabelMap
        ] ?? value,
      value,
    })),
  )

  const contractTypeOptions = computed(() =>
    Array.from(
      new Set(
        applicants.value
          .map(item => item.contractType)
          .filter((v) => !!v),
      ),
    ).map(value => ({
      label:
        applicantContractTypeLabelMap[
          value as keyof typeof applicantContractTypeLabelMap
        ] ?? value,
      value,
    })),
  )

  const toolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'dropdown',
      label: '問い合わせ年月',
      options: [{ label: 'すべて', value: '' }, ...yearMonthOptions.value],
      onSelect: (value: string) => {
        filter.contactYearMonth = value
      },
    },
    {
      type: 'dropdown',
      label: '媒体',
      options: [{ label: 'すべて', value: '' }, ...mediaOptions.value],
      onSelect: (value: string) => {
        filter.mediaName = value
      },
    },
    {
      type: 'dropdown',
      label: 'ステータス',
      options: [{ label: 'すべて', value: '' }, ...statusOptions.value],
      onSelect: (value: string) => {
        filter.retirementStatus = value
      },
    },
    {
      type: 'dropdown',
      label: '性別',
      options: [{ label: 'すべて', value: '' }, ...genderOptions.value],
      onSelect: (value: string) => {
        filter.gender = value
      },
    },
    {
      type: 'dropdown',
      label: '雇用形態',
      options: [{ label: 'すべて', value: '' }, ...contractTypeOptions.value],
      onSelect: (value: string) => {
        filter.contractType = value
      },
    },
  ])

  const filteredApplicants = computed(() =>
    applicants.value.filter(item => {
      const matchYearMonth =
        !filter.contactYearMonth ||
        item.contactDate?.slice(0, 7) === filter.contactYearMonth

      const matchMedia =
        !filter.mediaName || item.mediaName === filter.mediaName

      const matchStatus =
        !filter.retirementStatus || item.retirementStatus === filter.retirementStatus

      const matchGender =
        !filter.gender || item.gender === filter.gender

      const matchContractType =
        !filter.contractType || item.contractType === filter.contractType

      return (
        matchYearMonth &&
        matchMedia &&
        matchStatus &&
        matchGender &&
        matchContractType
      )
    }),
  )

  return {
    filter,
    toolbarItems,
    filteredApplicants,
  }
}