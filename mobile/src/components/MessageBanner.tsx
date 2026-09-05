import { StyleSheet, Text, View } from 'react-native';

import { colors, fonts, radii, spacing } from '@/src/constants/theme';

type MessageBannerProps = {
  message: string;
  tone?: 'error' | 'success';
};

export function MessageBanner({
  message,
  tone = 'error',
}: MessageBannerProps) {
  return (
    <View style={[styles.banner, tone === 'success' && styles.successBanner]}>
      <Text style={[styles.text, tone === 'success' && styles.successText]}>
        {message}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  banner: {
    backgroundColor: '#FDECEC',
    borderRadius: radii.sm,
    padding: spacing.md,
  },
  successBanner: {
    backgroundColor: '#E9F7F0',
  },
  text: {
    color: colors.danger,
    fontFamily: fonts.medium,
    fontSize: 13,
    lineHeight: 20,
  },
  successText: {
    color: colors.success,
  },
});
