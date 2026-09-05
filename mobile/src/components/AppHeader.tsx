import { useRouter } from 'expo-router';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, fonts, radii, spacing } from '@/src/constants/theme';

type AppHeaderProps = {
  title?: string;
};

export function AppHeader({ title }: AppHeaderProps) {
  const router = useRouter();
  return (
    <View style={styles.header}>
      <Pressable
        accessibilityLabel="Go back"
        accessibilityRole="button"
        hitSlop={10}
        onPress={() => router.back()}
        style={({ pressed }) => [styles.back, pressed && styles.pressed]}>
        <Text style={styles.arrow}>‹</Text>
      </Pressable>
      {title ? (
        <Text numberOfLines={1} style={styles.title}>
          {title}
        </Text>
      ) : (
        <View />
      )}
      <View style={styles.spacer} />
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    alignItems: 'center',
    flexDirection: 'row',
    minHeight: 52,
    paddingHorizontal: spacing.lg,
  },
  back: {
    alignItems: 'center',
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: radii.pill,
    borderWidth: 1,
    height: 40,
    justifyContent: 'center',
    width: 40,
  },
  pressed: { opacity: 0.68 },
  arrow: {
    color: colors.text,
    fontFamily: fonts.regular,
    fontSize: 34,
    lineHeight: 37,
    marginTop: -2,
  },
  title: {
    color: colors.text,
    flex: 1,
    fontFamily: fonts.semibold,
    fontSize: 16,
    marginHorizontal: spacing.md,
    textAlign: 'center',
  },
  spacer: { width: 40 },
});
