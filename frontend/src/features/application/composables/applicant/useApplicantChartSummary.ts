import { computed, type Ref } from 'vue'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import {
  applicantContractTypeLabelMap,
  applicantGenderLabelMap,
  applicantRetirementStatusLabelMap,
} from '@/features/application/constants/applicantConstants'

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
      if (item.recruitmentStatus === 'INTERVIEW') current.interviewed += 1
      if (item.retirementStatus === 'WORKING') current.working += 1
      if (item.retirementStatus === 'RESIGNED' || item.retirementStatus === 'BACKOUT') {
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
          value.applicants > 0
            ? Math.round((value.interviewed / value.applicants) * 100)
            : 0,
        workingRate:
          value.applicants > 0
            ? Math.round((value.working / value.applicants) * 100)
            : 0,
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
      if (item.retirementStatus === 'WORKING') current.working += 1
      if (item.retirementStatus === 'RESIGNED' || item.retirementStatus === 'BACKOUT') {
        current.resigned += 1
      }

      map.set(item.mediaName, current)
    }

    return Array.from(map.entries())
      .map(([mediaName, value]) => ({
        mediaName,
        ...value,
        workingRate:
          value.applicants > 0
            ? Math.round((value.working / value.applicants) * 100)
            : 0,
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