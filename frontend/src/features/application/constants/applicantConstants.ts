import type {
  ApplicantContractType,
  ApplicantGender,
  ApplicantRecruitmentStatus,
  ApplicantRetirementStatus,
  ApplicantRoomType,
  ApplicantYesNo,
} from '@/features/application/types/applicantTypes'

export const applicantYesNoLabelMap: Record<ApplicantYesNo, string> = {
  YES: 'あり',
  NO: 'なし',
}

export const applicantGenderLabelMap: Record<ApplicantGender, string> = {
  MALE: '男',
  FEMALE: '女',
  UNKNOWN: '不明',
}

export const applicantContractTypeLabelMap: Record<ApplicantContractType, string> = {
  PERMANENT: '正社員',
  CONTRACT: '契約社員',
  TEMPORARY: 'アルバイト',
  UNKNOWN: '不明',
}

export const applicantRoomTypeLabelMap: Record<Exclude<ApplicantRoomType, ''>, string> = {
  ONE: '1人部屋',
  SHARED: '複数人部屋',
  UNKNOWN: '不明',
}

export const applicantRecruitmentStatusLabelMap: Record<ApplicantRecruitmentStatus, string> = {
  JUST_CONTACT: '問い合わせのみ',
  WITHDRAW: '辞退',
  BACKOUT: '初日退職',
  INTERVIEW: '面接実施',
  HIRED: '採用',
  UNKNOWN: '不明',
}

export const applicantRetirementStatusLabelMap: Record<ApplicantRetirementStatus, string> = {
  WORKING: '在籍中',
  RESIGNED: '退職',
  BACKOUT: '初日退職',
  UNKNOWN: '不明',
}

export const applicantYesNoOptions = [
  { title: 'あり', value: 'YES' },
  { title: 'なし', value: 'NO' },
] as const

export const applicantGenderOptions = [
  { title: '男', value: 'MALE' },
  { title: '女', value: 'FEMALE' },
  { title: '不明', value: 'UNKNOWN' },
] as const

export const applicantContractTypeOptions = [
  { title: '正社員', value: 'PERMANENT' },
  { title: '契約社員', value: 'CONTRACT' },
  { title: 'アルバイト', value: 'TEMPORARY' },
  { title: '不明', value: 'UNKNOWN' },
] as const

export const applicantRoomTypeOptions = [
  { title: '1人部屋', value: 'ONE' },
  { title: '複数人部屋', value: 'SHARED' },
  { title: '不明', value: 'UNKNOWN' },
] as const

export const applicantRecruitmentStatusOptions = [
  { title: '問い合わせのみ', value: 'JUST_CONTACT' },
  { title: '面接実施', value: 'INTERVIEW' },
  { title: '採用', value: 'HIRED' },
  { title: '辞退', value: 'WITHDRAW' },
  { title: '初日退職', value: 'BACKOUT' },
]

export const applicantRetirementStatusOptions = [
  { title: '在籍中', value: 'WORKING' },
  { title: '退職', value: 'RESIGNED' },
  { title: '初日退職', value: 'BACKOUT' },
  { title: '不明', value: 'UNKNOWN' },
] as const