import { useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import {
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { getApiErrorMessage } from '@/src/api/errors';
import { fetchMembership, fetchProfile } from '@/src/api/profileApi';
import { useAuth } from '@/src/auth/AuthContext';
import { LoadingScreen } from '@/src/components/LoadingScreen';
import { MessageBanner } from '@/src/components/MessageBanner';
import { Screen } from '@/src/components/Screen';
import { colors, fonts, radii, spacing } from '@/src/constants/theme';
import type { Membership, Profile } from '@/src/types/api';

export default function HomeScreen() {
  const { user } = useAuth();
  const [profile, setProfile] = useState<Profile | null>(user);
  const [membership, setMembership] = useState<Membership | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadHome = useCallback(async (refreshing = false) => {
    refreshing ? setIsRefreshing(true) : setIsLoading(true);
    setError(null);
    try {
      const [nextProfile, nextMembership] = await Promise.all([
        fetchProfile(),
        fetchMembership(),
      ]);
      setProfile(nextProfile);
      setMembership(nextMembership);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to load your home page.'));
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      void loadHome();
    }, [loadHome]),
  );

  if (isLoading && !membership) {
    return <LoadingScreen label="Preparing your coffee dashboard…" />;
  }

  const membershipLabel = membership?.status.split('_').join(' ') ?? 'Unknown';

  return (
    <Screen padded={false}>
      <ScrollView
        contentContainerStyle={styles.content}
        refreshControl={
          <RefreshControl
            onRefresh={() => void loadHome(true)}
            refreshing={isRefreshing}
            tintColor={colors.primary}
          />
        }>
        <View style={styles.header}>
          <Text style={styles.eyebrow}>Social Cup</Text>
          <Text style={styles.title}>
            Welcome, {profile?.displayName ?? 'coffee lover'}
          </Text>
          <Text style={styles.subtitle}>
            Your neighbourhood coffee membership, all in one place.
          </Text>
        </View>

        {error ? <MessageBanner message={error} /> : null}

        <View style={styles.membershipCard}>
          <View style={styles.cardHeader}>
            <Text style={styles.cardLabel}>Membership</Text>
            <View
              style={[
                styles.badge,
                membership?.isMember && styles.activeBadge,
              ]}>
              <Text
                style={[
                  styles.badgeText,
                  membership?.isMember && styles.activeBadgeText,
                ]}>
                {membershipLabel}
              </Text>
            </View>
          </View>
          <Text style={styles.creditNumber}>
            {membership?.creditsRemaining ?? 0}
          </Text>
          <Text style={styles.creditLabel}>credits remaining</Text>
          {membership?.currentPeriodEnd ? (
            <Text style={styles.periodText}>
              Current period ends{' '}
              {new Date(membership.currentPeriodEnd).toLocaleDateString()}
            </Text>
          ) : null}
        </View>

        <View style={styles.connectionCard}>
          <View style={styles.connectionDot} />
          <View style={styles.connectionCopy}>
            <Text style={styles.connectionTitle}>Backend connected</Text>
            <Text style={styles.connectionBody}>
              Profile and membership data loaded securely with your JWT.
            </Text>
          </View>
        </View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { gap: spacing.xl, padding: spacing.xl, paddingBottom: spacing.xxxl },
  header: { gap: spacing.sm, paddingTop: spacing.lg },
  eyebrow: {
    color: colors.primary,
    fontFamily: fonts.semibold,
    fontSize: 13,
    letterSpacing: 1.2,
    textTransform: 'uppercase',
  },
  title: {
    color: colors.text,
    fontFamily: fonts.semibold,
    fontSize: 29,
    letterSpacing: -0.7,
    lineHeight: 37,
  },
  subtitle: {
    color: colors.textMuted,
    fontFamily: fonts.regular,
    fontSize: 15,
    lineHeight: 24,
  },
  membershipCard: {
    backgroundColor: colors.primary,
    borderRadius: radii.lg,
    padding: spacing.xl,
  },
  cardHeader: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  cardLabel: { color: colors.secondary, fontFamily: fonts.medium, fontSize: 14 },
  badge: {
    backgroundColor: 'rgba(255,255,255,0.12)',
    borderRadius: radii.pill,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
  },
  activeBadge: { backgroundColor: '#DFF5E9' },
  badgeText: { color: colors.secondary, fontFamily: fonts.semibold, fontSize: 11 },
  activeBadgeText: { color: colors.success },
  creditNumber: {
    color: colors.white,
    fontFamily: fonts.bold,
    fontSize: 54,
    lineHeight: 66,
    marginTop: spacing.xl,
  },
  creditLabel: { color: colors.secondary, fontFamily: fonts.medium, fontSize: 15 },
  periodText: {
    color: colors.secondary,
    fontFamily: fonts.regular,
    fontSize: 12,
    marginTop: spacing.lg,
    opacity: 0.88,
  },
  connectionCard: {
    alignItems: 'flex-start',
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: radii.md,
    borderWidth: 1,
    flexDirection: 'row',
    gap: spacing.md,
    padding: spacing.lg,
  },
  connectionDot: {
    backgroundColor: colors.success,
    borderRadius: radii.pill,
    height: 10,
    marginTop: 5,
    width: 10,
  },
  connectionCopy: { flex: 1, gap: spacing.xs },
  connectionTitle: { color: colors.text, fontFamily: fonts.semibold, fontSize: 14 },
  connectionBody: {
    color: colors.textMuted,
    fontFamily: fonts.regular,
    fontSize: 13,
    lineHeight: 20,
  },
});
