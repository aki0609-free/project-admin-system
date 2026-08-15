import { computed, type Ref } from 'vue'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import {
  applicantContractTypeLabelMap,
  applicantGenderLabelMap,
  applicantRetirementStatusLabelMap,
} from '@/features/application/constants/applicantConstants'
import {
  hasCompletedInterview,
  isResigned,
  isWorking,
  percentage,
} from '@/features/application/utils/applicantMetrics'

export const useApplicantChartSummary = (
  applicants: Ref<ApplicantRow[]>,
) => {
  const monthlySummary = computed(() => {
    const map = new Map<
      string,
      {
        applicants: number
        interviewed: number
        working: number
        resigned: number
      }
    >()

    for (const item of applicants.value) {
      const ym = item.contactDate?.slice(0, 7)
      if (!ym) continue

      const current = map.get(ym) ?? {
        applicants: 0,
        interviewed: 0,
        working: 0,
        resigned: 0,
      }

      current.applicants += 1
      if (hasCompletedInterview(item.recruitmentStatus)) current.interviewed += 1
      if (isWorking(item.retirementStatus)) current.working += 1
      if (isResigned(item.retirementStatus)) {
        current.resigned += 1
      }

      map.set(ym, current)
    }

    return Array.from(map.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([yearMonth, value]) => ({
        yearMonth,
        ...value,
        interviewRate:
          percentage(value.interviewed, value.applicants),
        workingRate:
          percentage(value.working, value.applicants),
      }))
  })

  const mediaSummary = computed(() => {
    const map = new Map<
      string,
      {
        applicants: number
        working: number
        resigned: number
      }
    >()

    for (const item of applicants.value) {
      if (!item.mediaName) continue

      const current = map.get(item.mediaName) ?? {
        applicants: 0,
        working: 0,
        resigned: 0,
      }

      current.applicants += 1
      if (isWorking(item.retirementStatus)) current.working += 1
      if (isResigned(item.retirementStatus)) {
        current.resigned += 1
      }

      map.set(item.mediaName, current)
    }

    return Array.from(map.entries())
      .map(([mediaName, value]) => ({
        mediaName,
        ...value,
        workingRate:
          percentage(value.working, value.applicants),
      }))
      .sort((a, b) => a.mediaName.localeCompare(b.mediaName))
  })

  const statusSummary = computed(() => {
    const map = new Map<string, number>()

    for (const item of applicants.value) {
      if (!item.retirementStatus) continue
      map.set(item.retirementStatus, (map.get(item.retirementStatus) ?? 0) + 1)
    }

    return Array.from(map.entries()).map(([status, count]) => ({
      status,
      label:
        applicantRetirementStatusLabelMap[
          status as keyof typeof applicantRetirementStatusLabelMap
        ] ?? status,
      count,
    }))
  })

  const genderSummary = computed(() => {
    const map = new Map<string, number>()

    for (const item of applicants.value) {
      if (!item.gender) continue
      map.set(item.gender, (map.get(item.gender) ?? 0) + 1)
    }

    return Array.from(map.entries()).map(([gender, count]) => ({
      gender,
      label:
        applicantGenderLabelMap[
          gender as keyof typeof applicantGenderLabelMap
        ] ?? gender,
      count,
    }))
  })

  const contractTypeSummary = computed(() => {
    const map = new Map<string, number>()

    for (const item of applicants.value) {
      if (!item.contractType) continue
      map.set(item.contractType, (map.get(item.contractType) ?? 0) + 1)
    }

    return Array.from(map.entries()).map(([contractType, count]) => ({
      contractType,
      label:
        applicantContractTypeLabelMap[
          contractType as keyof typeof applicantContractTypeLabelMap
        ] ?? contractType,
      count,
    }))
  })

  return {
    monthlySummary,
    mediaSummary,
    statusSummary,
    genderSummary,
    contractTypeSummary,
  }
}
