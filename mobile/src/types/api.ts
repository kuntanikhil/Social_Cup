export type Neighbourhood = {
  id: number;
  name: string;
};

export type CoffeePreference = {
  id: number;
  code: string;
  displayName: string;
};

export type Profile = {
  id: number;
  email: string;
  displayName: string;
  accountStatus: string;
  homeNeighbourhood: Neighbourhood | null;
  coffeePreferences: CoffeePreference[];
  onboardingCompleted: boolean;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type RegisterRequest = LoginRequest & {
  displayName: string;
};

export type AuthTokens = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
};

export type Membership = {
  status:
    | 'NONE'
    | 'INCOMPLETE'
    | 'ACTIVE'
    | 'PAYMENT_FAILED'
    | 'CANCEL_AT_PERIOD_END'
    | 'ENDED';
  isMember: boolean;
  creditsRemaining: number;
  currentPeriodEnd: string | null;
  cancelAtPeriodEnd: boolean;
};

export type DiscoverCafe = {
  id: number;
  name: string;
  neighbourhood: string;
  address: string;
  perkLine: string | null;
  featured: boolean;
  minimumCreditPrice: number | null;
  distanceKm: number | null;
  preferenceMatch: boolean;
  averageRating: number | null;
  ratingCount: number;
  newCafe: boolean;
};

export type FeaturedCafe = {
  id: number;
  name: string;
  neighbourhood: string;
  perkLine: string | null;
  minimumCreditPrice: number | null;
  averageRating: number | null;
  ratingCount: number;
  newCafe: boolean;
};

export type SignatureDrink = {
  id: number;
  name: string;
  type: string;
  creditPrice: number;
  photoPath: string | null;
  cafe: {
    id: number;
    name: string;
  };
};

export type DiscoverResponse = {
  featuredCafes: FeaturedCafe[];
  signatureDrinks: SignatureDrink[];
  cafes: DiscoverCafe[];
};

export type CafePhoto = {
  id: number;
  storagePath: string;
  displayOrder: number;
};

export type CafeOpeningHours = {
  id: number;
  dayOfWeek: number;
  opensAt: string | null;
  closesAt: string | null;
  closed: boolean;
};

export type Drink = {
  id: number;
  name: string;
  type: string;
  description: string | null;
  photoPath: string | null;
  retailPrice: number;
  creditPrice: number;
  signature: boolean;
};

export type CafeDetail = {
  id: number;
  name: string;
  address: string;
  neighbourhood: Neighbourhood;
  latitude: number | null;
  longitude: number | null;
  perkLine: string | null;
  featured: boolean;
  averageRating: number | null;
  ratingCount: number;
  newCafe: boolean;
  photos: CafePhoto[];
  openingHours: CafeOpeningHours[];
  drinks: Drink[];
};

export type RedemptionCafe = {
  id: number;
  name: string;
};

export type RedemptionDrink = {
  id: number;
  name: string;
  credits: number;
};

export type RedemptionStatus =
  | 'PENDING'
  | 'REDEEMED'
  | 'EXPIRED'
  | 'CANCELLED';

export type CreateRedemptionSessionResponse = {
  sessionId: number;
  cafe: RedemptionCafe;
  drink: RedemptionDrink;
  creditsBefore: number;
  creditsAfter: number;
  qrToken: string;
  backupCode: string;
  expiresAt: string;
  expiresInSeconds: number;
  status: RedemptionStatus;
};

export type RedemptionSession = {
  sessionId: number;
  cafe: RedemptionCafe;
  drink: Omit<RedemptionDrink, 'credits'>;
  credits: number;
  status: RedemptionStatus;
  expiresAt: string;
  serverTime: string;
};
