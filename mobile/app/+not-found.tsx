import { Link } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';

import { colors, fonts, spacing } from '@/src/constants/theme';

export default function NotFoundScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>We couldn’t find that page.</Text>
      <Link href="/" style={styles.link}>
        Return to Social Cup
      </Link>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    backgroundColor: colors.surface,
    flex: 1,
    gap: spacing.lg,
    justifyContent: 'center',
    padding: spacing.xl,
  },
  title: {
    color: colors.text,
    fontFamily: fonts.semibold,
    fontSize: 22,
    textAlign: 'center',
  },
  link: {
    color: colors.primary,
    fontFamily: fonts.semibold,
  },
});
