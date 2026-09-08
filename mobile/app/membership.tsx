import {
  PaymentSheetError,
  useStripe,
} from '@stripe/stripe-react-native';
import { Redirect, useLocalSearchParams, useRouter } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { getApiErrorMessage, getHttpStatus } from '@/src/api/errors';
import {
  createMembershipCheckout,
  getMembership,
  reconcileMembership,
} from '@/src/api/membershipApi';
import { useAuth } from '@/src/auth/AuthContext';
import { AppHeader } from '@/src/components/AppHeader';
import { LoadingScreen } from '@/src/components/LoadingScreen';
import { MessageBanner } from '@/src/components/MessageBanner';
import { PrimaryButton } from '@/src/components/PrimaryButton';
import { Screen } from '@/src/components/Screen';
import {
  isStripeConfigured,
  STRIPE_RETURN_URL,
} from '@/src/constants/stripe';
import { colors, fonts, radii, spacing } from '@/src/constants/theme';
import type { Membership, StripeCheckoutResponse } from '@/src/types/api';

const ACTIVATION_POLL_INTERVAL_MS = 1_500;
const ACTIVATION_TIMEOUT_MS = 24_000;

type ActivationPhase = 'idle' | 'activating' | 'delayed' | 'complete';

export default function MembershipScreen() {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) return <LoadingScreen />;
  if (!isAuthenticated) return <Redirect href="/(auth)/login" />;
  if (!isStripeConfigured) return <MissingStripeConfiguration />;

  return <MembershipPaymentScreen />;
}

function MembershipPaymentScreen() {
  const { cafeId: cafeIdParam } = useLocalSearchParams<{ cafeId?: string }>();
  const cafeId = Number(cafeIdParam);
  const hasRedeemDestination = Number.isInteger(cafeId) && cafeId > 0;
  const router = useRouter();
  const { initPaymentSheet, presentPaymentSheet } = useStripe();
  const alive = useRef(true);
  const paymentInFlight = useRef(false);
  const pollingRun = useRef(0);
  const [membership, setMembership] = useState<Membership | null>(null);
  const [pendingCheckout, setPendingCheckout] =
    useState<StripeCheckoutResponse | null>(null);
  const [phase, setPhase] = useState<ActivationPhase>('idle');
  const [isLoading, setIsLoading] = useState(true);
  const [isPaying, setIsPaying] = useState(false);
  const [isChecking, setIsChecking] = useState(false);
  const [paymentSubmitted, setPaymentSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    alive.current = true;
    return () => {
      alive.current = false;
      pollingRun.current += 1;
    };
  }, []);

  const loadMembership = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const nextMembership = await getMembership();
      if (!alive.current) return;
      setMembership(nextMembership);
      if (nextMembership.isMember) setPhase('complete');
    } catch (requestError) {
      if (!alive.current) return;
      setError(
        getApiErrorMessage(
          requestError,
          'Unable to load your membership right now.',
        ),
      );
    } finally {
      if (alive.current) setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadMembership();
  }, [loadMembership]);

  const pollForActivation = useCallback(async () => {
    const run = ++pollingRun.current;
    const deadline = Date.now() + ACTIVATION_TIMEOUT_MS;
    setPhase('activating');
    setNotice(null);
    setError(null);

    while (Date.now() < deadline && alive.current && run === pollingRun.current) {
      try {
        const nextMembership = await getMembership();
        if (!alive.current || run !== pollingRun.current) return;
        setMembership(nextMembership);

        if (nextMembership.isMember) {
          setPhase('complete');
          return;
        }
        if (nextMembership.status === 'PAYMENT_FAILED') {
          setPhase('idle');
          setError(
            'Stripe could not complete the payment. Please check your payment method and try again.',
          );
          return;
        }
        if (nextMembership.status === 'ENDED') {
          setPhase('idle');
          setError('This subscription is no longer active. You can start a new subscription.');
          return;
        }
      } catch {
        // A later poll may succeed; the final state handles a prolonged outage.
      }

      await wait(ACTIVATION_POLL_INTERVAL_MS);
    }

    if (alive.current && run === pollingRun.current) {
      setPhase('delayed');
      setNotice(
        "Payment received. We're finishing your membership activation.",
      );
    }
  }, []);

  const subscribe = async () => {
    if (paymentInFlight.current) return;
    paymentInFlight.current = true;
    setIsPaying(true);
    setError(null);
    setNotice(null);

    try {
      let checkout = pendingCheckout;
      if (!checkout) {
        checkout = await createMembershipCheckout();
        if (!isValidCheckout(checkout)) {
          throw new Error('The checkout response was incomplete. Please try again.');
        }
        if (alive.current) setPendingCheckout(checkout);
      }

      const { error: initializationError } = await initPaymentSheet({
        merchantDisplayName: 'Social Cup',
        paymentIntentClientSecret: checkout.clientSecret,
        customerEphemeralKeySecret: checkout.ephemeralKey,
        customerId: checkout.customerId,
        returnURL: STRIPE_RETURN_URL,
      });

      if (initializationError) {
        setError(
          'Unable to prepare the secure payment form. Please try again.',
        );
        return;
      }

      const { didCancel, error: paymentError } = await presentPaymentSheet();
      if (didCancel || paymentError) {
        if (didCancel || paymentError?.code === PaymentSheetError.Canceled) {
          setNotice(
            'Payment was cancelled. Your membership and credits have not changed.',
          );
        } else if (paymentError?.code === PaymentSheetError.Timeout) {
          setError('The payment form timed out. Please try again.');
        } else {
          setError(
            'Payment could not be completed. Check your card details and try again.',
          );
        }
        await refreshAfterUnfinishedPayment();
        return;
      }

      if (!alive.current) return;
      setPendingCheckout(null);
      setPaymentSubmitted(true);
      await pollForActivation();
    } catch (requestError) {
      if (!alive.current) return;
      if (getHttpStatus(requestError) === 409) {
        setError(
          'A Stripe checkout already exists for this membership. Check its payment status below.',
        );
      } else {
        setError(
          getApiErrorMessage(
            requestError,
            'Unable to start membership checkout. Please try again.',
          ),
        );
      }
    } finally {
      paymentInFlight.current = false;
      if (alive.current) setIsPaying(false);
    }
  };

  const refreshAfterUnfinishedPayment = async () => {
    try {
      const nextMembership = await getMembership();
      if (alive.current) setMembership(nextMembership);
    } catch {
      // The user-facing payment result remains useful if this refresh fails.
    }
  };

  const checkActivation = async () => {
    if (isChecking || paymentInFlight.current) return;
    setIsChecking(true);
    setError(null);
    setNotice(null);

    try {
      let nextMembership = await getMembership();
      if (!alive.current) return;
      setMembership(nextMembership);

      if (!nextMembership.isMember && nextMembership.status === 'INCOMPLETE') {
        // Recovery fallback only: normal activation is always webhook + GET polling.
        nextMembership = await reconcileMembership();
        if (!alive.current) return;
        setMembership(nextMembership);
      }

      if (nextMembership.isMember) {
        setPhase('complete');
      } else {
        setPhase(paymentSubmitted ? 'delayed' : 'idle');
        setNotice(
          nextMembership.status === 'PAYMENT_FAILED'
            ? 'Stripe reports a payment problem. Please retry with a valid payment method.'
            : 'Membership is not active yet. If you just paid, wait a moment and check again.',
        );
      }
    } catch (requestError) {
      if (!alive.current) return;
      setError(
        getApiErrorMessage(
          requestError,
          'Unable to check membership activation right now.',
        ),
      );
    } finally {
      if (alive.current) setIsChecking(false);
    }
  };

  const continueFromMembership = () => {
    if (hasRedeemDestination) {
      router.replace({
        pathname: '/redeem/[cafeId]',
        params: { cafeId: String(cafeId) },
      });
    } else {
      router.replace('/(tabs)/discover');
    }
  };

  if (isLoading && !membership) {
    return <LoadingScreen label="Loading membership…" />;
  }

  const isMember = membership?.isMember === true;
  const canStartNewCheckout =
    membership?.status === 'NONE' || membership?.status === 'ENDED';
  const canResumeCheckout =
    pendingCheckout !== null && membership?.status !== 'ENDED';
  const showSubscribe =
    !isMember &&
    phase !== 'activating' &&
    phase !== 'delayed' &&
    (canStartNewCheckout || canResumeCheckout);

  return (
    <Screen padded={false}>
      <AppHeader title="Membership" />
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.heading}>
          <Text style={styles.eyebrow}>Social Cup Membership</Text>
          <Text style={styles.title}>30 coffee credits every month</Text>
          <View style={styles.priceRow}>
            <Text style={styles.price}>$24.99</Text>
            <Text style={styles.pricePeriod}>/ month</Text>
          </View>
        </View>

        {error ? <MessageBanner message={error} /> : null}
        {notice ? <MessageBanner message={notice} tone="success" /> : null}

        {!membership ? (
          <View style={styles.statusCard}>
            <Text style={styles.statusTitle}>Membership unavailable</Text>
            <Text style={styles.statusCopy}>
              Check your connection, then try loading your membership again.
            </Text>
            <PrimaryButton
              isLoading={isLoading}
              label="Retry"
              onPress={() => void loadMembership()}
              variant="secondary"
            />
          </View>
        ) : null}

        {isMember && membership ? (
          <View style={styles.successCard}>
            <View style={styles.successMark}>
              <Text style={styles.successMarkText}>✓</Text>
            </View>
            <Text style={styles.successTitle}>You&apos;re a Social Cup member</Text>
            <Text style={styles.successCopy}>
              {membership.creditsRemaining} credits are ready to use.
            </Text>
            {membership.currentPeriodEnd ? (
              <Text style={styles.renewalText}>
                {membership.cancelAtPeriodEnd ? 'Access ends' : 'Renews'}{' '}
                {formatDate(membership.currentPeriodEnd)}
              </Text>
            ) : null}
            <View style={styles.fullWidth}>
              <PrimaryButton
                label={hasRedeemDestination ? 'Continue to Redeem' : 'Explore Cafes'}
                onPress={continueFromMembership}
              />
            </View>
          </View>
        ) : phase === 'activating' ? (
          <View style={styles.activationCard}>
            <ActivityIndicator color={colors.primary} size="large" />
            <Text style={styles.activationTitle}>Activating your membership…</Text>
            <Text style={styles.activationCopy}>
              Stripe has received your payment. We&apos;re waiting for the secure backend confirmation and credit grant.
            </Text>
          </View>
        ) : phase === 'delayed' ? (
          <View style={styles.activationCard}>
            <Text style={styles.waitingIcon}>◷</Text>
            <Text style={styles.activationTitle}>Activation is taking a little longer</Text>
            <Text style={styles.activationCopy}>
              Your backend membership—not the app—decides when credits are available.
            </Text>
            <View style={styles.fullWidth}>
              <PrimaryButton
                isLoading={isChecking}
                label="Check Again"
                onPress={() => void checkActivation()}
                variant="secondary"
              />
            </View>
          </View>
        ) : null}

        <MembershipPlan />

        {!isMember && membership?.status === 'PAYMENT_FAILED' ? (
          <View style={styles.statusCard}>
            <Text style={styles.statusTitle}>Payment needs attention</Text>
            <Text style={styles.statusCopy}>
              We couldn&apos;t confirm the subscription payment. Check the status again after updating the payment in Stripe.
            </Text>
            <PrimaryButton
              isLoading={isChecking}
              label="Check Payment Status"
              onPress={() => void checkActivation()}
              variant="secondary"
            />
          </View>
        ) : null}

        {!isMember && membership?.status === 'INCOMPLETE' && !pendingCheckout && phase === 'idle' ? (
          <View style={styles.statusCard}>
            <Text style={styles.statusTitle}>Checkout in progress</Text>
            <Text style={styles.statusCopy}>
              A subscription checkout already exists. If payment was completed, verify its status with Stripe.
            </Text>
            <PrimaryButton
              isLoading={isChecking}
              label="Check Payment Status"
              onPress={() => void checkActivation()}
              variant="secondary"
            />
          </View>
        ) : null}

        {showSubscribe ? (
          <View style={styles.subscribeBlock}>
            <PrimaryButton
              isLoading={isPaying}
              label={
                canResumeCheckout
                  ? 'Resume Secure Payment'
                  : 'Subscribe for $24.99/month'
              }
              onPress={() => void subscribe()}
            />
            <Text style={styles.secureNote}>
              Card details are entered securely in Stripe PaymentSheet. Social Cup never stores them.
            </Text>
          </View>
        ) : null}
      </ScrollView>
    </Screen>
  );
}

function MembershipPlan() {
  return (
    <View style={styles.planCard}>
      <Benefit text="30 credits monthly" />
      <Benefit text="Redeem at partner cafes" />
      <Benefit text="Discover signature drinks" />
      <Benefit text="Cancel anytime" />
      <View style={styles.planDivider} />
      <Text style={styles.planFinePrint}>
        1 credit = $1 member value · Credits reset monthly · No rollover · No top-ups
      </Text>
    </View>
  );
}

function Benefit({ text }: { text: string }) {
  return (
    <View style={styles.benefitRow}>
      <View style={styles.benefitMark}>
        <Text style={styles.benefitMarkText}>✓</Text>
      </View>
      <Text style={styles.benefitText}>{text}</Text>
    </View>
  );
}

function MissingStripeConfiguration() {
  const router = useRouter();
  return (
    <Screen padded={false}>
      <AppHeader title="Membership" />
      <View style={styles.missingContainer}>
        <MessageBanner message="Stripe payments are not configured in this app build. Set EXPO_PUBLIC_STRIPE_PUBLISHABLE_KEY and create a new native build." />
        <PrimaryButton
          label="Back to Discover"
          onPress={() => router.replace('/(tabs)/discover')}
          variant="secondary"
        />
      </View>
    </Screen>
  );
}

function isValidCheckout(checkout: StripeCheckoutResponse): boolean {
  return [
    checkout.subscriptionId,
    checkout.clientSecret,
    checkout.ephemeralKey,
    checkout.customerId,
  ].every((value) => typeof value === 'string' && value.trim().length > 0);
}

function formatDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'after the current period';
  return new Intl.DateTimeFormat(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(date);
}

function wait(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

const styles = StyleSheet.create({
  content: { gap: spacing.xl, padding: spacing.xl, paddingBottom: spacing.xxxl },
  heading: { gap: spacing.xs },
  eyebrow: {
    color: colors.primary,
    fontFamily: fonts.semibold,
    fontSize: 12,
    letterSpacing: 1,
    textTransform: 'uppercase',
  },
  title: {
    color: colors.text,
    fontFamily: fonts.bold,
    fontSize: 29,
    lineHeight: 38,
  },
  priceRow: { alignItems: 'baseline', flexDirection: 'row', gap: spacing.xs, marginTop: spacing.sm },
  price: { color: colors.primary, fontFamily: fonts.bold, fontSize: 40, lineHeight: 50 },
  pricePeriod: { color: colors.textMuted, fontFamily: fonts.medium, fontSize: 15 },
  planCard: {
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: radii.lg,
    borderWidth: 1,
    gap: spacing.md,
    padding: spacing.xl,
  },
  benefitRow: { alignItems: 'center', flexDirection: 'row', gap: spacing.md },
  benefitMark: {
    alignItems: 'center',
    backgroundColor: '#E9F7F0',
    borderRadius: radii.pill,
    height: 28,
    justifyContent: 'center',
    width: 28,
  },
  benefitMarkText: { color: colors.success, fontFamily: fonts.bold, fontSize: 14 },
  benefitText: { color: colors.text, flex: 1, fontFamily: fonts.medium, fontSize: 14 },
  planDivider: { backgroundColor: colors.border, height: 1, marginVertical: spacing.xs },
  planFinePrint: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 11, lineHeight: 18 },
  subscribeBlock: { gap: spacing.md },
  secureNote: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 11, lineHeight: 18, paddingHorizontal: spacing.sm, textAlign: 'center' },
  activationCard: {
    alignItems: 'center',
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: radii.lg,
    borderWidth: 1,
    gap: spacing.md,
    padding: spacing.xl,
  },
  activationTitle: { color: colors.text, fontFamily: fonts.semibold, fontSize: 18, textAlign: 'center' },
  activationCopy: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 13, lineHeight: 21, textAlign: 'center' },
  waitingIcon: { color: colors.warning, fontFamily: fonts.bold, fontSize: 35 },
  successCard: {
    alignItems: 'center',
    backgroundColor: colors.card,
    borderColor: '#B7DFC9',
    borderRadius: radii.lg,
    borderWidth: 1,
    gap: spacing.sm,
    padding: spacing.xl,
  },
  successMark: { alignItems: 'center', backgroundColor: '#E9F7F0', borderRadius: radii.pill, height: 58, justifyContent: 'center', width: 58 },
  successMarkText: { color: colors.success, fontFamily: fonts.bold, fontSize: 29 },
  successTitle: { color: colors.text, fontFamily: fonts.bold, fontSize: 21, marginTop: spacing.sm, textAlign: 'center' },
  successCopy: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 14, lineHeight: 22, textAlign: 'center' },
  renewalText: { color: colors.primary, fontFamily: fonts.semibold, fontSize: 12, marginTop: spacing.xs },
  fullWidth: { marginTop: spacing.md, width: '100%' },
  statusCard: { backgroundColor: colors.card, borderColor: colors.border, borderRadius: radii.md, borderWidth: 1, gap: spacing.md, padding: spacing.lg },
  statusTitle: { color: colors.text, fontFamily: fonts.semibold, fontSize: 16 },
  statusCopy: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 13, lineHeight: 20 },
  missingContainer: { flex: 1, gap: spacing.lg, justifyContent: 'center', padding: spacing.xl },
});
