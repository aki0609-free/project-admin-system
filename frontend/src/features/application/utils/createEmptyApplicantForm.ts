import { ApplicantPersistedRow } from "../types/applicantTypes";

export function createEmptyApplicant(length: number): ApplicantPersistedRow {
  return {
    id: Date.now(),
    no: length + 1,

    name: '',
    furiganaName: '',
    contractType: 'UNKNOWN',
    retirementStatus: 'UNKNOWN',
    birthDate: null,
    gender: 'UNKNOWN',

    joinDateWithoutInsurance: null,
    leaveDateWithoutInsurance: null,
    joinDateWithInsurance: null,
    leaveDateWithInsurance: null,

    contactDate: null,
    recruitmentStatus: 'UNKNOWN',
    sourceWhere: '',
    searchSite: '',
    keyword: '',
    repostedTo: '',
    applicationReason1: '',
    applicationReason2: '',

    recruitmentCompany: '',
    mediaType: '',
    mediaName: '',
    mediaArea: '',
    mediaSlots: null,
    recruitmentUnitPrice: null,

    dailyWageAtJoin: null,
    needsDormitory: 'NO',
    roomType: 'UNKNOWN',
    dormitoryFee: null,
    previousJob: '',
    previousJobPeriod: '',
    insuredBefore: 'NO',
    dormitoryExperience: 'NO',
    previousDormitoryFee: null,

    clientBeforeLeave: '',
    industryBeforeLeave: '',
    estimatedRetirementReason: '',
  }
}