export type DailyPreparationStatus = 'OPEN' | 'COMPLETED'
export type VehicleArrangementType = 'NONE' | 'COMPANY' | 'EMPLOYEE' | 'PASSENGER'

export type DailyPreparationAssignmentResponse = {
  id: number
  preparationId: number

  employeeId: number
  employeeCode: string
  employeeName: string

  customerId: number | null
  customerSiteId: number | null
  customerName: string | null
  siteName: string | null

  vehicleArrangementType: VehicleArrangementType
  passengerCount: number

  workDescription: string | null
}

export type DailyPreparationDispatchResponse = {
  id: number
  preparationId: number

  customerId: number | null
  customerSiteId: number
  customerName: string | null
  siteName: string | null

  distanceFromCompanyKm: number | null
  vehicleCount: number
  otherAmount: number

  note: string | null
}

export type DailyPreparationResponse = {
  id: number
  targetDate: string
  status: DailyPreparationStatus
  note: string | null

  assignments: DailyPreparationAssignmentResponse[]
  dispatches: DailyPreparationDispatchResponse[]
}

export type DailyPreparationCreateRequest = {
  targetDate: string
  note: string | null
}

export type DailyPreparationAssignmentSaveRequest = {
  preparationId: number
  employeeId: number
  customerId: number | null
  customerSiteId: number | null
  vehicleArrangementType: VehicleArrangementType
  passengerCount: number
  workDescription: string | null
}

export type DailyPreparationAssignmentBulkSaveItemRequest = {
  id: number | null
  employeeId: number
  customerId: number | null
  customerSiteId: number | null
  vehicleArrangementType: VehicleArrangementType
  passengerCount: number
  workDescription: string | null
  isNew: boolean
  isUpdated: boolean
  isDeleted: boolean
}

export type DailyPreparationAssignmentBulkSaveRequest = {
  preparationId: number
  items: DailyPreparationAssignmentBulkSaveItemRequest[]
}

export type DailyPreparationDispatchBulkSaveItemRequest = {
  id: number | null

  customerId: number | null
  customerSiteId: number | null

  distanceFromCompanyKm: number
  vehicleCount: number
  otherAmount: number
  note: string | null

  isNew: boolean
  isUpdated: boolean
  isDeleted: boolean
}

export type DailyPreparationDispatchBulkSaveRequest = {
  preparationId: number
  items: DailyPreparationDispatchBulkSaveItemRequest[]
}
