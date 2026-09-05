import { Redirect, useLocalSearchParams, useRouter } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { fetchCafe } from '@/src/api/cafeApi';
import { getApiErrorMessage, getHttpStatus } from '@/src/api/errors';
import { fetchMembership } from '@/src/api/profileApi';
import { createRedemptionSession } from '@/src/api/redemptionApi';
import { useAuth } from '@/src/auth/AuthContext';
import { AppHeader } from '@/src/components/AppHeader';
import { EmptyState } from '@/src/components/EmptyState';
import { LoadingScreen } from '@/src/components/LoadingScreen';
import { MessageBanner } from '@/src/components/MessageBanner';
import { PrimaryButton } from '@/src/components/PrimaryButton';
import { Screen } from '@/src/components/Screen';
import { colors, fonts, radii, spacing } from '@/src/constants/theme';
import { useRedemptionSession } from '@/src/redemption/RedemptionSessionContext';
import type { CafeDetail, Drink, Membership } from '@/src/types/api';

export default function RedeemDrinkScreen() {
  const { cafeId: cafeIdParam } = useLocalSearchParams<{ cafeId: string }>();
  const cafeId = Number(cafeIdParam);
  const router = useRouter();
  const { isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const { setActiveSession } = useRedemptionSession();
  const [cafe, setCafe] = useState<CafeDetail | null>(null);
  const [membership, setMembership] = useState<Membership | null>(null);
  const [selectedDrink, setSelectedDrink] = useState<Drink | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!Number.isInteger(cafeId) || cafeId <= 0) {
      setError('This cafe link is invalid.');
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const [nextCafe, nextMembership] = await Promise.all([
        fetchCafe(cafeId),
        fetchMembership(),
      ]);
      setCafe(nextCafe);
      setMembership(nextMembership);
    } catch (requestError) {
      setError(
        getHttpStatus(requestError) === 404
          ? 'This cafe is no longer available.'
          : getApiErrorMessage(requestError, 'Unable to prepare redemption.'),
      );
    } finally {
      setIsLoading(false);
    }
  }, [cafeId]);

  useEffect(() => {
    if (isAuthenticated) void load();
  }, [isAuthenticated, load]);

  const confirm = async () => {
    if (!selectedDrink || !membership) return;
    setIsSubmitting(true);
    setError(null);
    try {
      const session = await createRedemptionSession(cafeId, selectedDrink.id);
      setActiveSession(session);
      router.replace({
        pathname: '/redemption/[sessionId]',
        params: { sessionId: String(session.sessionId) },
      });
    } catch (requestError) {
      const status = getHttpStatus(requestError);
      if (status === 409) {
        setError('You do not have enough credits for this drink.');
      } else if (status === 403) {
        setError('An active paid membership is required to redeem.');
        void fetchMembership().then(setMembership).catch(() => undefined);
      } else if (status === 404) {
        setError('The selected cafe or drink is no longer available.');
      } else {
        setError(getApiErrorMessage(requestError, 'Unable to create your redemption code.'));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isAuthLoading) return <LoadingScreen />;
  if (!isAuthenticated) return <Redirect href="/(auth)/login" />;
  if (isLoading) return <LoadingScreen label="Checking membership and menu…" />;

  const isActiveMember = membership?.status === 'ACTIVE' && membership.isMember;
  const creditsRemaining = membership?.creditsRemaining ?? 0;
  const canAfford = selectedDrink ? creditsRemaining >= selectedDrink.creditPrice : false;

  return (
    <Screen padded={false}>
      <AppHeader title="Redeem a drink" />
      <ScrollView contentContainerStyle={styles.content}>
        {error ? <MessageBanner message={error} /> : null}

        {!isActiveMember ? (
          <View style={styles.membershipRequired}>
            <Text style={styles.lock}>●</Text>
            <Text style={styles.title}>Membership required</Text>
            <Text style={styles.body}>An active Social Cup membership is needed before you can create a redemption code.</Text>
            <View style={styles.balancePill}><Text style={styles.balancePillText}>Current status: {membership?.status.split('_').join(' ') ?? 'Unavailable'}</Text></View>
            <PrimaryButton label="Check again" onPress={() => void load()} variant="secondary" />
          </View>
        ) : cafe ? (
          <>
            <View style={styles.heading}>
              <Text style={styles.eyebrow}>{cafe.name}</Text>
              <Text style={styles.title}>Choose your drink</Text>
              <Text style={styles.body}>Your balance is checked again by the backend when the barista validates your code.</Text>
            </View>

            <View style={styles.balanceCard}>
              <Text style={styles.balanceLabel}>Available balance</Text>
              <Text style={styles.balanceValue}>{creditsRemaining} credits</Text>
            </View>

            {cafe.drinks.length ? (
              <View style={styles.drinkList}>
                {cafe.drinks.map((drink) => {
                  const selected = selectedDrink?.id === drink.id;
                  const affordable = creditsRemaining >= drink.creditPrice;
                  return (
                    <Pressable
                      accessibilityRole="radio"
                      accessibilityState={{ checked: selected, disabled: !affordable }}
                      disabled={!affordable}
                      key={drink.id}
                      onPress={() => setSelectedDrink(drink)}
                      style={({ pressed }) => [styles.drinkCard, selected && styles.drinkCardSelected, !affordable && styles.drinkCardDisabled, pressed && styles.pressed]}>
                      <View style={[styles.radio, selected && styles.radioSelected]}>{selected ? <View style={styles.radioDot} /> : null}</View>
                      <View style={styles.drinkCopy}>
                        <Text style={styles.drinkName}>{drink.name}</Text>
                        <Text style={styles.drinkType}>{drink.type.split('_').join(' ')}</Text>
                        {!affordable ? <Text style={styles.unavailable}>Not enough credits</Text> : null}
                      </View>
                      <Text style={styles.drinkCredits}>{drink.creditPrice} credits</Text>
                    </Pressable>
                  );
                })}
              </View>
            ) : (
              <EmptyState message="There are no active drinks available to redeem." title="No drinks available" />
            )}

            {selectedDrink ? (
              <View style={styles.confirmation}>
                <Text style={styles.confirmTitle}>Confirm redemption</Text>
                <SummaryRow label="Drink" value={selectedDrink.name} />
                <SummaryRow label="Credit cost" value={`${selectedDrink.creditPrice}`} />
                <SummaryRow label="Current balance" value={`${creditsRemaining}`} />
                <View style={styles.divider} />
                <SummaryRow label="Balance after redemption" strong value={`${creditsRemaining - selectedDrink.creditPrice}`} />
                <Text style={styles.previewNote}>Preview only. Credits are deducted only after the barista validates your code.</Text>
                <PrimaryButton disabled={!canAfford} isLoading={isSubmitting} label="Confirm and generate code" onPress={() => void confirm()} />
              </View>
            ) : null}
          </>
        ) : null}
      </ScrollView>
    </Screen>
  );
}

function SummaryRow({ label, strong, value }: { label: string; strong?: boolean; value: string }) {
  return <View style={styles.summaryRow}><Text style={[styles.summaryLabel, strong && styles.strong]}>{label}</Text><Text style={[styles.summaryValue, strong && styles.strong]}>{value}</Text></View>;
}

const styles = StyleSheet.create({
  content: { gap: spacing.xl, padding: spacing.xl, paddingBottom: spacing.xxxl },
  heading: { gap: spacing.xs },
  eyebrow: { color: colors.primary, fontFamily: fonts.semibold, fontSize: 12, letterSpacing: 0.8, textTransform: 'uppercase' },
  title: { color: colors.text, fontFamily: fonts.bold, fontSize: 27, lineHeight: 35 },
  body: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 14, lineHeight: 22 },
  balanceCard: { backgroundColor: colors.primary, borderRadius: radii.md, padding: spacing.lg },
  balanceLabel: { color: colors.secondary, fontFamily: fonts.medium, fontSize: 12 },
  balanceValue: { color: colors.white, fontFamily: fonts.bold, fontSize: 25, marginTop: spacing.xs },
  drinkList: { gap: spacing.md },
  drinkCard: { alignItems: 'center', backgroundColor: colors.card, borderColor: colors.border, borderRadius: radii.md, borderWidth: 1, flexDirection: 'row', gap: spacing.md, minHeight: 78, padding: spacing.md },
  drinkCardSelected: { borderColor: colors.primary, borderWidth: 2 },
  drinkCardDisabled: { opacity: 0.5 },
  pressed: { opacity: 0.75 },
  radio: { alignItems: 'center', borderColor: colors.border, borderRadius: radii.pill, borderWidth: 2, height: 22, justifyContent: 'center', width: 22 },
  radioSelected: { borderColor: colors.primary },
  radioDot: { backgroundColor: colors.primary, borderRadius: radii.pill, height: 10, width: 10 },
  drinkCopy: { flex: 1 },
  drinkName: { color: colors.text, fontFamily: fonts.semibold, fontSize: 14 },
  drinkType: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 11, marginTop: 2, textTransform: 'capitalize' },
  unavailable: { color: colors.danger, fontFamily: fonts.medium, fontSize: 10, marginTop: 2 },
  drinkCredits: { color: colors.primary, fontFamily: fonts.semibold, fontSize: 13 },
  confirmation: { backgroundColor: colors.card, borderColor: colors.border, borderRadius: radii.lg, borderWidth: 1, gap: spacing.md, padding: spacing.lg },
  confirmTitle: { color: colors.text, fontFamily: fonts.semibold, fontSize: 18 },
  summaryRow: { alignItems: 'flex-start', flexDirection: 'row', gap: spacing.md, justifyContent: 'space-between' },
  summaryLabel: { color: colors.textMuted, flex: 1, fontFamily: fonts.regular, fontSize: 13 },
  summaryValue: { color: colors.text, fontFamily: fonts.medium, fontSize: 13, textAlign: 'right' },
  strong: { color: colors.text, fontFamily: fonts.semibold, fontSize: 14 },
  divider: { backgroundColor: colors.border, height: 1 },
  previewNote: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 11, lineHeight: 17 },
  membershipRequired: { alignItems: 'center', backgroundColor: colors.card, borderColor: colors.border, borderRadius: radii.lg, borderWidth: 1, gap: spacing.md, padding: spacing.xl },
  lock: { color: colors.warning, fontSize: 28 },
  balancePill: { backgroundColor: colors.secondary, borderRadius: radii.pill, paddingHorizontal: spacing.md, paddingVertical: spacing.sm },
  balancePillText: { color: colors.primary, fontFamily: fonts.semibold, fontSize: 11 },
});
