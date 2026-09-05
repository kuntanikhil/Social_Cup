import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { getApiErrorMessage } from '@/src/api/errors';
import { useAuth } from '@/src/auth/AuthContext';
import { MessageBanner } from '@/src/components/MessageBanner';
import { PrimaryButton } from '@/src/components/PrimaryButton';
import { Screen } from '@/src/components/Screen';
import { colors, fonts, radii, spacing } from '@/src/constants/theme';

export default function ProfileScreen() {
  const { logout, user } = useAuth();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleLogout() {
    setError(null);
    setIsLoggingOut(true);
    try {
      await logout();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to sign out cleanly.'));
    } finally {
      setIsLoggingOut(false);
    }
  }

  return (
    <Screen>
      <View style={styles.header}>
        <Text style={styles.eyebrow}>Your account</Text>
        <Text style={styles.title}>Profile</Text>
      </View>
      {error ? <MessageBanner message={error} /> : null}
      <View style={styles.card}>
        <View style={styles.initialCircle}>
          <Text style={styles.initial}>
            {user?.displayName.trim().charAt(0).toUpperCase() || 'S'}
          </Text>
        </View>
        <View style={styles.profileCopy}>
          <Text style={styles.name}>{user?.displayName}</Text>
          <Text style={styles.email}>{user?.email}</Text>
          <Text style={styles.meta}>
            {user?.homeNeighbourhood?.name ?? 'Neighbourhood not selected'}
          </Text>
        </View>
      </View>
      <View style={styles.statusRow}>
        <Text style={styles.statusLabel}>Onboarding</Text>
        <Text style={styles.statusValue}>
          {user?.onboardingCompleted ? 'Complete' : 'Not complete'}
        </Text>
      </View>
      <PrimaryButton
        isLoading={isLoggingOut}
        label="Sign Out"
        onPress={() => void handleLogout()}
        variant="secondary"
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { gap: spacing.xs, marginBottom: spacing.xl, marginTop: spacing.lg },
  eyebrow: {
    color: colors.primary,
    fontFamily: fonts.semibold,
    fontSize: 12,
    letterSpacing: 1.1,
    textTransform: 'uppercase',
  },
  title: { color: colors.text, fontFamily: fonts.semibold, fontSize: 30 },
  card: {
    alignItems: 'center',
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: radii.lg,
    borderWidth: 1,
    flexDirection: 'row',
    gap: spacing.lg,
    marginBottom: spacing.lg,
    padding: spacing.xl,
  },
  initialCircle: {
    alignItems: 'center',
    backgroundColor: colors.secondary,
    borderRadius: radii.pill,
    height: 56,
    justifyContent: 'center',
    width: 56,
  },
  initial: { color: colors.primary, fontFamily: fonts.bold, fontSize: 23 },
  profileCopy: { flex: 1, gap: spacing.xs },
  name: { color: colors.text, fontFamily: fonts.semibold, fontSize: 18 },
  email: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 13 },
  meta: { color: colors.primary, fontFamily: fonts.medium, fontSize: 12 },
  statusRow: {
    alignItems: 'center',
    borderBottomColor: colors.border,
    borderBottomWidth: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: spacing.xl,
    paddingVertical: spacing.lg,
  },
  statusLabel: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 14 },
  statusValue: { color: colors.text, fontFamily: fonts.semibold, fontSize: 14 },
});
