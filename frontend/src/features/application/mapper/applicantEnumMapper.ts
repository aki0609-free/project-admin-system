import type {
  ApplicantContractType,
  ApplicantGender,
  ApplicantRecruitmentStatus,
  ApplicantRetirementStatus,
  ApplicantRoomType,
  ApplicantYesNo,
} from '@/features/application/types/applicantTypes'

function safeEnum<T extends string>(
  value: string | null | undefined,
  allowed: readonly T[],
  fallback: T,
): T {
  return (allowed as readonly string[]).includes(value ?? '')
    ? (value as T)
    : fallback
}

const YES_NO_VALUES = ['YES', 'NO'] as const
const GENDER_VALUES = ['MALE', 'FEMALE'] as const
const CONTRACT_TYPE_VALUES = ['PERMANENT', 'CONTRACT', 'TEMPORARY'] as const
const ROOM_TYPE_VALUES = ['ONE', 'SHARED', 'UNKNOWN'] as const
const RETIREMENT_STATUS_VALUES = ['WORKING', 'RESIGNED', 'BACKOUT'] as const
const RECRUITMENT_STATUS_VALUES = [
  'JUST_CONTACT',
  'WITHDRAW',
  'BACKOUT',
  'INTERVIEW',
  'HIRED',
] as const

export function toApplicantYesNo(
  value: string | null | undefined,
): ApplicantYesNo {
  return safeEnum(value, YES_NO_VALUES, 'NO')
}

export function toApplicantGender(
  value: string | null | undefined,
): ApplicantGender {
  return safeEnum(value, GENDER_VALUES, 'UNKNOWN')
}

export function toApplicantContractType(
  value: string | null | undefined,
): ApplicantContractType {
  return safeEnum(value, CONTRACT_TYPE_VALUES, 'UNKNOWN')
}

export function toApplicantRoomType(
  value: string | null | undefined,
): ApplicantRoomType {
  return safeEnum(value, ROOM_TYPE_VALUES, 'UNKNOWN')
}

export function toApplicantRecruitmentStatus(
  value: string | null | undefined,
): ApplicantRecruitmentStatus {
  return safeEnum(value, RECRUITMENT_STATUS_VALUES, 'UNKNOWN')
}

export function toApplicantRetirementStatus(
  value: string | null | undefined,
): ApplicantRetirementStatus {
  return safeEnum(value, RETIREMENT_STATUS_VALUES, 'UNKNOWN')
}