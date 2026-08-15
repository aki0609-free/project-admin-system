import type {
  ApplicantRecruitmentStatus,
  ApplicantRetirementStatus,
} from '@/features/application/types/applicantTypes'

/**
 * 採用状況は現在地点を保持するため、面接後の状態も面接実施済みとして扱う。
 */
export function hasCompletedInterview(
  recruitmentStatus: ApplicantRecruitmentStatus,
): boolean {
  return recruitmentStatus === 'INTERVIEW'
    || recruitmentStatus === 'HIRED'
    || recruitmentStatus === 'BACKOUT'
}

export function isWorking(
  retirementStatus: ApplicantRetirementStatus,
): boolean {
  return retirementStatus === 'WORKING'
}

export function isResigned(
  retirementStatus: ApplicantRetirementStatus,
): boolean {
  return retirementStatus === 'RESIGNED'
    || retirementStatus === 'BACKOUT'
}

export function percentage(
  numerator: number,
  denominator: number,
): number {
  if (denominator <= 0) {
    return 0
  }

  return Math.round((numerator / denominator) * 100)
}

export function nextApplicantNumber(
  applicationNumbers: number[],
): number {
  const validNumbers = applicationNumbers.filter(
    value => Number.isInteger(value) && value > 0,
  )

  return validNumbers.length === 0
    ? 1
    : Math.max(...validNumbers) + 1
}
