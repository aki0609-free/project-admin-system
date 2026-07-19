export type DormitoryInfoRequest = {
  needsDormitory: string | null
  roomType: string | null
  rent: string | null
}

export type SearchKeywordInfoRequest = {
  searchPlace: string | null
  searchWebsite: string | null
  searchKeyword: string | null
  desiredNextJob: string | null
}

export type PreviousJobInfoRequest = {
  previousJob: string | null
  previousJobPeriod: string | null
  insuredBefore: string | null
  dormitoryExperience: string | null
  previousRent: string | null
}

export type ApplicationMediaLinkRequest = {
  id: number | null
  mediaName: string | null
  mediaArea: string | null
  mediaSlots: number | null
  mediaYearMonth: string | null
}

export type ApplicationMediaLinkResponse = {
  id: number | null
  mediaName: string | null
  mediaArea: string | null
  mediaSlots: number | null
  mediaYearMonth: string | null
  unitPrice: number | null
}

export type ApplicantListItemResponse = {
  id: number
  applicationNo: string
  contractType: string | null
  name: string
  furiganaName: string | null
  birthDate: string | null
  gender: string | null
  contactDate: string | null
  recruitmentStatus: string | null
  dailyWage: string | null
  employmentDate: string | null
  employmentDateWithInsurance: string | null
  terminationDate: string | null
  terminationDateWithInsurance: string | null
  jobCategory: string | null
  previousClient: string | null
  resignationStatus: string | null
  resignationReason: string | null
  applicationReason1: string | null
  applicationReason2: string | null
  recruitmentCompany: string | null
  mediaType: string | null
  dormitoryInfo: DormitoryInfoRequest | null
  searchKeywordInfo: SearchKeywordInfoRequest | null
  previousJobInfo: PreviousJobInfoRequest | null
  applicationMedia: ApplicationMediaLinkResponse | null
}

export type ApplicantDetailResponse = ApplicantListItemResponse

export type ApplicantCreateRequest = {
  applicationNo: string
  contractType: string | null
  name: string
  furiganaName: string | null
  birthDate: string | null
  gender: string | null
  contactDate: string | null
  recruitmentStatus: string | null
  dailyWage: string | null
  employmentDate: string | null
  employmentDateWithInsurance: string | null
  terminationDate: string | null
  terminationDateWithInsurance: string | null
  jobCategory: string | null
  previousClient: string | null
  resignationStatus: string | null
  resignationReason: string | null
  applicationReason1: string | null
  applicationReason2: string | null
  recruitmentCompany: string | null
  mediaType: string | null
  dormitoryInfo: DormitoryInfoRequest | null
  searchKeywordInfo: SearchKeywordInfoRequest | null
  previousJobInfo: PreviousJobInfoRequest | null
  applicationMedia: ApplicationMediaLinkRequest | null
}

export type ApplicantUpdateRequest = ApplicantCreateRequest & {
  id: number
}