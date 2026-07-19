export type ApplicantChartFilter = {
  contactYearMonth: string
  mediaName: string
  retirementStatus: string
  gender: string
}

export type ApplicantChartBase =
  | 'ALL'
  | 'INTERVIEWED'
  | 'WORKING'
  | 'RESIGNED'
