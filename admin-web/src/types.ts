export type AuthResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export type Neighbourhood = { id: number; name: string }

export type AdminProfile = {
  id: number
  email: string
  displayName: string
  accountStatus: string
  role: 'MEMBER' | 'ADMIN'
}

export type AdminCafe = {
  id: number
  name: string
  address: string
  neighbourhoodId: number
  neighbourhoodName: string
  latitude: number | null
  longitude: number | null
  perkLine: string | null
  payoutRatePerCredit: number
  featured: boolean
  active: boolean
  scanSlug: string
  createdAt: string
  updatedAt: string
}

export type CafeWriteRequest = {
  name: string
  address: string
  neighbourhoodId: number
  latitude: number | null
  longitude: number | null
  perkLine: string | null
  payoutRatePerCredit: number
  featured: boolean
  active?: boolean
}

export type DrinkType = 'MATCHA' | 'ESPRESSO' | 'COLD_BREW' | 'LATTE'

export type AdminDrink = {
  id: number
  cafeId: number
  name: string
  type: DrinkType
  description: string | null
  photoPath: string | null
  retailPrice: number
  creditPrice: number
  signature: boolean
  active: boolean
  createdAt: string
  updatedAt: string
}

export type DrinkWriteRequest = {
  name: string
  type: DrinkType
  description: string | null
  photoPath: string | null
  retailPrice: number
  creditPrice: number
  signature: boolean
  active?: boolean
}

export type AdminRedemption = {
  redemptionId: number
  redeemedAt: string
  memberName: string
  cafeId: number
  cafeName: string
  drinkId: number
  drinkName: string
  creditsSpent: number
  status: 'COMPLETED'
  payoutAmount: number
}

export type PayoutSummary = {
  cafeId: number
  cafeName: string
  eligibleRedemptions: number
  creditsRedeemed: number
  payoutAmount: number
}
