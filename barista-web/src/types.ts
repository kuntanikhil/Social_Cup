export type DeviceSession = {
  cafeId: number
  cafeName: string
  deviceToken: string
  expiresAt: string | null
}

export type DeviceAuthenticationResponse = DeviceSession

export type BaristaFailureReason =
  | 'INVALID_CODE'
  | 'CODE_EXPIRED'
  | 'CODE_ALREADY_USED'
  | 'WRONG_CAFE'
  | 'MEMBERSHIP_INACTIVE'
  | 'INSUFFICIENT_CREDITS'
  | 'DEVICE_UNAUTHORIZED'

export type BaristaFailureResponse = {
  result: 'FAILURE'
  reason: BaristaFailureReason
}

export type BaristaRedemptionResponse = {
  result: 'SUCCESS'
  redemptionId: number
  member: {
    firstName: string
    profilePhoto: string | null
  }
  drink: {
    id: number
    name: string
  }
  creditsDeducted: number
  creditsRemaining: number
}

export type TodayRedemption = {
  redemptionId: number
  memberFirstName: string
  drinkName: string
  creditsSpent: number
  redeemedAt: string
}

export type ValidationResult =
  | { kind: 'success'; data: BaristaRedemptionResponse }
  | { kind: 'failure'; reason: BaristaFailureReason }
  | { kind: 'network-error' }
