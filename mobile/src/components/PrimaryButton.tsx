import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  type PressableProps,
} from 'react-native';

import { colors, fonts, radii, spacing } from '@/src/constants/theme';

type PrimaryButtonProps = PressableProps & {
  label: string;
  isLoading?: boolean;
  variant?: 'primary' | 'secondary';
};

export function PrimaryButton({
  label,
  isLoading = false,
  variant = 'primary',
  disabled,
  style,
  ...props
}: PrimaryButtonProps) {
  const isDisabled = disabled || isLoading;

  return (
    <Pressable
      accessibilityRole="button"
      disabled={isDisabled}
      style={(state) => [
        styles.button,
        variant === 'secondary' && styles.secondary,
        state.pressed && !isDisabled && styles.pressed,
        isDisabled && styles.disabled,
        typeof style === 'function' ? style(state) : style,
      ]}
      {...props}>
      {isLoading ? (
        <ActivityIndicator
          color={variant === 'primary' ? colors.white : colors.primary}
        />
      ) : (
        <Text
          style={[
            styles.label,
            variant === 'secondary' && styles.secondaryLabel,
          ]}>
          {label}
        </Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    alignItems: 'center',
    backgroundColor: colors.primary,
    borderRadius: radii.md,
    justifyContent: 'center',
    minHeight: 52,
    paddingHorizontal: spacing.lg,
  },
  secondary: {
    backgroundColor: colors.secondary,
  },
  pressed: {
    opacity: 0.86,
  },
  disabled: {
    opacity: 0.55,
  },
  label: {
    color: colors.white,
    fontFamily: fonts.semibold,
    fontSize: 16,
  },
  secondaryLabel: {
    color: colors.primary,
  },
});
