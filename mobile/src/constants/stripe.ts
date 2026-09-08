export const STRIPE_PUBLISHABLE_KEY =
  process.env.EXPO_PUBLIC_STRIPE_PUBLISHABLE_KEY?.trim() ?? '';

export const STRIPE_URL_SCHEME = 'socialcup';
export const STRIPE_RETURN_URL = `${STRIPE_URL_SCHEME}://stripe-redirect`;

export const isStripeConfigured = STRIPE_PUBLISHABLE_KEY.length > 0;
