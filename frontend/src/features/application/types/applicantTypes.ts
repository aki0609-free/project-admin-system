export type ApplicantYesNo = 'YES' | 'NO'

export type ApplicantGender = 'MALE' | 'FEMALE' | 'UNKNOWN'

export type ApplicantContractType =
  | 'PERMANENT'
  | 'CONTRACT'
  | 'TEMPORARY'
  | 'UNKNOWN'

export type ApplicantRoomType =
  | 'ONE'
  | 'SHARED'
  | 'UNKNOWN'

export type ApplicantRecruitmentStatus =
  | 'JUST_CONTACT'
  | 'WITHDRAW'
  | 'BACKOUT'
  | 'INTERVIEW'
  | 'HIRED'
  | 'UNKNOWN'

export type ApplicantRetirementStatus =
  | 'WORKING'
  | 'RESIGNED'
  | 'BACKOUT'
  | 'UNKNOWN'

export type ApplicantPersistedRow = {
  id: number
  no: number

  name: string
  furiganaName: string
  contractType: ApplicantContractType
  retirementStatus: ApplicantRetirementStatus
  birthDate: string | null
  gender: ApplicantGender

  joinDateWithoutInsurance: string | null
  leaveDateWithoutInsurance: string | null
  joinDateWithInsurance: string | null
  leaveDateWithInsurance: string | null

  contactDate: string | null
  recruitmentStatus: ApplicantRecruitmentStatus
  sourceWhere: string
  searchSite: string
  keyword: string
  repostedTo: string
  applicationReason1: string
  applicationReason2: string

  recruitmentCompany: string
  mediaType: string
  mediaName: string
  mediaArea: string
  mediaSlots: number | null
  recruitmentUnitPrice: number | null

  dailyWageAtJoin: number | null
  needsDormitory: ApplicantYesNo
  roomType: ApplicantRoomType
  dormitoryFee: number | null
  previousJob: string
  previousJobPeriod: string
  insuredBefore: ApplicantYesNo
  dormitoryExperience: ApplicantYesNo
  previousDormitoryFee: number | null

  clientBeforeLeave: string
  industryBeforeLeave: string
  estimatedRetirementReason: string
}

export type ApplicantRow = ApplicantPersistedRow & {
  ageAtJoin?: number | null
  ageAtLeave?: number | null
  currentTenure?: string
  tenureWithoutInsurance?: string
  tenureWithInsurance?: string
}