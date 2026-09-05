import { useKeepAwake } from 'expo-keep-awake';
import { Redirect, useLocalSearchParams, useRouter } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import QRCode from 'react-native-qrcode-svg';

import { getApiErrorMessage, getHttpStatus } from '@/src/api/errors';
import { fetchMembership } from '@/src/api/profileApi';
import { fetchRedemptionSession } from '@/src/api/redemptionApi';
import { useAuth } from '@/src/auth/AuthContext';
import { AppHeader } from '@/src/components/AppHeader';
import { LoadingScreen } from '@/src/components/LoadingScreen';
import { MessageBanner } from '@/src/components/MessageBanner';
import { PrimaryButton } from '@/src/components/PrimaryButton';
import { Screen } from '@/src/components/Screen';
import { colors, fonts, radii, spacing } from '@/src/constants/theme';
import { useRedemptionSession } from '@/src/redemption/RedemptionSessionContext';
import type { RedemptionSession, RedemptionStatus } from '@/src/types/api';

const POLL_INTERVAL_MS = 2_000;

export default function RedemptionCodeScreen() {
  const { sessionId: sessionIdParam } = useLocalSearchParams<{ sessionId: string }>();
  const sessionId = Number(sessionIdParam);
  const router = useRouter();
  const { isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const { activeSession, clearActiveSession } = useRedemptionSession();
  const oneTimeSession = activeSession?.sessionId === sessionId ? activeSession : null;
  const [session, setSession] = useState<RedemptionSession | null>(null);
  const [secondsRemaining, setSecondsRemaining] = useState(oneTimeSession?.expiresInSeconds ?? 0);
  const [isLoading, setIsLoading] = useState(true);
  const [updatedBalance, setUpdatedBalance] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const timerAnchor = useRef({ seconds: oneTimeSession?.expiresInSeconds ?? 0, clientTime: Date.now() });

  const syncServerClock = useCallback((nextSession: RedemptionSession) => {
    const expiresAt = Date.parse(nextSession.expiresAt);
    const serverTime = Date.parse(nextSession.serverTime);
    const seconds = Number.isFinite(expiresAt) && Number.isFinite(serverTime)
      ? Math.max(0, Math.ceil((expiresAt - serverTime) / 1_000))
      : 0;
    timerAnchor.current = { seconds, clientTime: Date.now() };
    setSecondsRemaining(seconds);
  }, []);

  const poll = useCallback(async () => {
    if (!Number.isInteger(sessionId) || sessionId <= 0) {
      setError('This redemption session link is invalid.');
      setIsLoading(false);
      return;
    }
    try {
      const nextSession = await fetchRedemptionSession(sessionId);
      setSession(nextSession);
      syncServerClock(nextSession);
      setError(null);
    } catch (requestError) {
      setError(
        getHttpStatus(requestError) === 404
          ? 'This redemption session could not be found.'
          : getApiErrorMessage(requestError, 'Unable to check this redemption session.'),
      );
    } finally {
      setIsLoading(false);
    }
  }, [sessionId, syncServerClock]);

  useEffect(() => {
    if (isAuthenticated) void poll();
  }, [isAuthenticated, poll]);

  const status: RedemptionStatus = session?.status ?? oneTimeSession?.status ?? 'PENDING';
  const isTerminal = status === 'REDEEMED' || status === 'EXPIRED' || status === 'CANCELLED';

  useEffect(() => {
    if (!isAuthenticated || isTerminal) return;
    let requestInFlight = false;
    const interval = setInterval(() => {
      if (requestInFlight) return;
      requestInFlight = true;
      void poll().finally(() => {
        requestInFlight = false;
      });
    }, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [isAuthenticated, isTerminal, poll]);

  useEffect(() => {
    if (isTerminal) return;
    const interval = setInterval(() => {
      const elapsed = Math.floor((Date.now() - timerAnchor.current.clientTime) / 1_000);
      setSecondsRemaining(Math.max(0, timerAnchor.current.seconds - elapsed));
    }, 1_000);
    return () => clearInterval(interval);
  }, [isTerminal]);

  useEffect(() => {
    if (status !== 'REDEEMED') return;
    void fetchMembership()
      .then((membership) => setUpdatedBalance(membership.creditsRemaining))
      .catch(() => setUpdatedBalance(null));
  }, [status]);

  useEffect(() => {
    if (isTerminal) clearActiveSession();
  }, [clearActiveSession, isTerminal]);

  if (isAuthLoading) return <LoadingScreen />;
  if (!isAuthenticated) return <Redirect href="/(auth)/login" />;
  if (isLoading && !oneTimeSession && !session) return <LoadingScreen label="Checking your redemption code…" />;

  const cafe = session?.cafe ?? oneTimeSession?.cafe;
  const drink = session?.drink ?? oneTimeSession?.drink;
  const credits = session?.credits ?? oneTimeSession?.drink.credits;
  const visuallyExpired = status === 'EXPIRED' || secondsRemaining <= 0;

  return (
    <Screen padded={false}>
      <AppHeader title="Redemption code" />
      <ScrollView contentContainerStyle={styles.content}>
        {error ? <MessageBanner message={error} /> : null}

        {!cafe || !drink ? (
          <ResultState
            actionLabel="Back to Discover"
            message="This code cannot be restored from device storage. Create a new session when your connection is available."
            onPress={() => router.replace('/(tabs)/discover')}
            symbol="i"
            title="Code unavailable"
          />
        ) : status === 'REDEEMED' ? (
          <ResultState
            actionLabel="Back to Discover"
            message={updatedBalance === null ? `${credits ?? 0} credits deducted. Refresh Home to see your current balance.` : `${credits ?? 0} credits deducted · ${updatedBalance} credits remaining`}
            onPress={() => router.replace('/(tabs)/discover')}
            symbol="✓"
            title="Redeemed"
          >
            <Text style={styles.resultDrink}>{drink?.name}</Text>
            <Text style={styles.resultCafe}>{cafe?.name}</Text>
          </ResultState>
        ) : visuallyExpired ? (
          <ResultState
            actionLabel="Return to cafe"
            message="This five-minute code has expired. Create a new code when you are ready at the counter."
            onPress={() => cafe && router.replace({ pathname: '/cafe/[id]', params: { id: String(cafe.id) } })}
            symbol="!"
            title="Code expired"
          />
        ) : status === 'CANCELLED' ? (
          <ResultState
            actionLabel="Return to cafe"
            message="This code was replaced by a newer redemption session and can no longer be used."
            onPress={() => cafe && router.replace({ pathname: '/cafe/[id]', params: { id: String(cafe.id) } })}
            symbol="×"
            title="Code cancelled"
          />
        ) : oneTimeSession ? (
          <ActiveCode
            backupCode={oneTimeSession.backupCode}
            cafeName={cafe?.name ?? ''}
            drinkName={drink?.name ?? ''}
            qrToken={oneTimeSession.qrToken}
            secondsRemaining={secondsRemaining}
          />
        ) : (
          <ResultState
            actionLabel="Return to cafe"
            message="For security, QR and backup codes are shown only once and are not stored on this device. Create a new session to get another code."
            onPress={() => cafe && router.replace({ pathname: '/cafe/[id]', params: { id: String(cafe.id) } })}
            symbol="i"
            title="Code no longer available"
          />
        )}
      </ScrollView>
    </Screen>
  );
}

function ActiveCode({
  backupCode,
  cafeName,
  drinkName,
  qrToken,
  secondsRemaining,
}: {
  backupCode: string;
  cafeName: string;
  drinkName: string;
  qrToken: string;
  secondsRemaining: number;
}) {
  useKeepAwake();
  return (
    <>
      <View style={styles.heading}>
        <Text style={styles.eyebrow}>Ready to redeem</Text>
        <Text style={styles.title}>{drinkName}</Text>
        <Text style={styles.cafeName}>{cafeName}</Text>
      </View>
      <View style={styles.codeCard}>
        <View accessibilityLabel="Redemption QR code" style={styles.qrShell}>
          <QRCode backgroundColor={colors.white} color={colors.text} quietZone={8} size={238} value={qrToken} />
        </View>
        <Text style={styles.instruction}>Show this code to the barista</Text>
        <View style={styles.timerPill}><Text style={styles.timer}>{formatCountdown(secondsRemaining)}</Text></View>
        <View style={styles.divider} />
        <Text style={styles.backupLabel}>Backup code</Text>
        <Text accessibilityLabel={`Backup code ${backupCode.split('').join(' ')}`} selectable style={styles.backupCode}>{backupCode}</Text>
      </View>
      <Text style={styles.safetyNote}>Your balance changes only after the barista successfully validates this code.</Text>
    </>
  );
}

function ResultState({
  actionLabel,
  children,
  message,
  onPress,
  symbol,
  title,
}: React.PropsWithChildren<{
  actionLabel: string;
  message: string;
  onPress: () => void;
  symbol: string;
  title: string;
}>) {
  return (
    <View style={styles.resultCard}>
      <View style={styles.resultSymbol}><Text style={styles.resultSymbolText}>{symbol}</Text></View>
      <Text style={styles.resultTitle}>{title}</Text>
      {children}
      <Text style={styles.resultMessage}>{message}</Text>
      <View style={styles.resultAction}><PrimaryButton label={actionLabel} onPress={onPress} /></View>
    </View>
  );
}

function formatCountdown(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return `${minutes}:${String(remainder).padStart(2, '0')}`;
}

const styles = StyleSheet.create({
  content: { gap: spacing.xl, padding: spacing.xl, paddingBottom: spacing.xxxl },
  heading: { alignItems: 'center', gap: spacing.xs },
  eyebrow: { color: colors.success, fontFamily: fonts.semibold, fontSize: 11, letterSpacing: 1, textTransform: 'uppercase' },
  title: { color: colors.text, fontFamily: fonts.bold, fontSize: 25, lineHeight: 33, textAlign: 'center' },
  cafeName: { color: colors.textMuted, fontFamily: fonts.medium, fontSize: 14, textAlign: 'center' },
  codeCard: { alignItems: 'center', backgroundColor: colors.card, borderColor: colors.border, borderRadius: radii.lg, borderWidth: 1, gap: spacing.md, padding: spacing.xl },
  qrShell: { backgroundColor: colors.white, borderColor: colors.secondary, borderRadius: radii.md, borderWidth: 1, padding: spacing.sm },
  instruction: { color: colors.text, fontFamily: fonts.semibold, fontSize: 14, marginTop: spacing.sm, textAlign: 'center' },
  timerPill: { backgroundColor: '#F4DFBD', borderRadius: radii.pill, paddingHorizontal: spacing.lg, paddingVertical: spacing.sm },
  timer: { color: colors.primary, fontFamily: fonts.bold, fontSize: 20, fontVariant: ['tabular-nums'] },
  divider: { backgroundColor: colors.border, height: 1, marginVertical: spacing.sm, width: '100%' },
  backupLabel: { color: colors.textMuted, fontFamily: fonts.semibold, fontSize: 11, letterSpacing: 0.8, textTransform: 'uppercase' },
  backupCode: { color: colors.text, fontFamily: fonts.bold, fontSize: 34, letterSpacing: 8, lineHeight: 44, textAlign: 'center' },
  safetyNote: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 12, lineHeight: 19, paddingHorizontal: spacing.md, textAlign: 'center' },
  resultCard: { alignItems: 'center', backgroundColor: colors.card, borderColor: colors.border, borderRadius: radii.lg, borderWidth: 1, gap: spacing.md, marginTop: spacing.xl, padding: spacing.xl },
  resultSymbol: { alignItems: 'center', backgroundColor: '#E9F7F0', borderRadius: radii.pill, height: 72, justifyContent: 'center', width: 72 },
  resultSymbolText: { color: colors.success, fontFamily: fonts.bold, fontSize: 37 },
  resultTitle: { color: colors.text, fontFamily: fonts.bold, fontSize: 25, textAlign: 'center' },
  resultDrink: { color: colors.text, fontFamily: fonts.semibold, fontSize: 17, textAlign: 'center' },
  resultCafe: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 14, textAlign: 'center' },
  resultMessage: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 13, lineHeight: 21, textAlign: 'center' },
  resultAction: { marginTop: spacing.sm, width: '100%' },
});
