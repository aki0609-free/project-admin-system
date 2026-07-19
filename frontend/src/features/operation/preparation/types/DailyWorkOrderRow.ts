export type DailyWorkOrderRow = {
  id: number

  targetDate: string

  employeeId: number
  employeeCode: string
  employeeName: string

  customerId: number
  customerName: string

  customerSiteId: number
  siteName: string

  jobType: string
  workDescription: string

  startTime: string
  endTime: string

  workHours: number
  overtimeHours: number
  nightWorkHours: number

  privateCarUsed: boolean
  passengerCount: number

  vehicleCount: number

  commuteRoute: string
  oneWayDistanceKm: number
  commuteCost: number

  note: string
}