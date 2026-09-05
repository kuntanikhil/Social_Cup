import { StyleSheet, Text, View } from 'react-native';

import { colors, fonts, radii, spacing } from '@/src/constants/theme';

type SectionPlaceholderProps = {
  eyebrow: string;
  title: string;
  body: string;
};

export function SectionPlaceholder({
  eyebrow,
  title,
  body,
}: SectionPlaceholderProps) {
  return (
    <View style={styles.container}>
      <Text style={styles.eyebrow}>{eyebrow}</Text>
      <Text style={styles.title}>{title}</Text>
      <Text style={styles.body}>{body}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: radii.lg,
    borderWidth: 1,
    gap: spacing.md,
    marginTop: spacing.xxl,
    padding: spacing.xl,
  },
  eyebrow: {
    color: colors.primary,
    fontFamily: fonts.semibold,
    fontSize: 12,
    letterSpacing: 1.2,
    textTransform: 'uppercase',
  },
  title: {
    color: colors.text,
    fontFamily: fonts.semibold,
    fontSize: 24,
    lineHeight: 31,
  },
  body: {
    color: colors.textMuted,
    fontFamily: fonts.regular,
    fontSize: 15,
    lineHeight: 24,
  },
});
