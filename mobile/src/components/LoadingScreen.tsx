import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';

import { colors, fonts, spacing } from '@/src/constants/theme';

type LoadingScreenProps = {
  label?: string;
};

export function LoadingScreen({ label = 'Loading Social Cup…' }: LoadingScreenProps) {
  return (
    <View style={styles.container}>
      <ActivityIndicator color={colors.primary} size="large" />
      <Text style={styles.label}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    backgroundColor: colors.surface,
    flex: 1,
    gap: spacing.md,
    justifyContent: 'center',
    padding: spacing.xl,
  },
  label: {
    color: colors.textMuted,
    fontFamily: fonts.medium,
    fontSize: 14,
  },
});
