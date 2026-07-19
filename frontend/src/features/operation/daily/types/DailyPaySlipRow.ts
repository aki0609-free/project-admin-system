export type DailyPaySlipRow = {
  id: number

  employeeCode: string
  employeeName: string

  paymentDate: string
  workDateFrom: string
  workDateTo: string

  baseSalary: number
  overtimeAllowance: number
  nightAllowance: number
  attitudeAllowance: number
  drivingAllowance: number
  managementAllowance: number
  otherAllowance: number
  totalPaymentAmount: number

  legalDepositAmount: number
  legalDepositBalance: number
  totalDeduction1Amount: number

  advancePaymentAmount: number
  advancePaymentBalance: number
  savingAmount: number
  savingBalance: number
  loanRepaymentAmount: number
  dormitoryFee: number
  dormitoryRemainingDays: number
  mobileRentalFee: number
  wifiFee: number
  totalDeduction2Amount: number

  netPaymentAmount: number

  note: string | null
}