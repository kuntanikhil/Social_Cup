import { Link, router } from 'expo-router';
import { useState } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getApiErrorMessage } from '@/src/api/errors';
import { useAuth } from '@/src/auth/AuthContext';
import { FormField } from '@/src/components/FormField';
import { MessageBanner } from '@/src/components/MessageBanner';
import { PrimaryButton } from '@/src/components/PrimaryButton';
import { colors, fonts, radii, spacing } from '@/src/constants/theme';

export default function LoginScreen() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleLogin() {
    setError(null);
    setIsSubmitting(true);
    try {
      await login({ email: email.trim(), password });
      router.replace('/(tabs)');
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to sign in.'));
    } finally {
      setIsSubmitting(false);
    }
  }

  const canSubmit = email.trim().length > 0 && password.length >= 8;

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.flex}>
        <ScrollView
          contentContainerStyle={styles.scrollContent}
          keyboardShouldPersistTaps="handled">
          <View style={styles.brandBlock}>
            <Text style={styles.brand}>Social Cup</Text>
            <Text style={styles.tagline}>
              Your neighbourhood coffee membership.
            </Text>
          </View>

          <View style={styles.card}>
            <View style={styles.headingBlock}>
              <Text style={styles.title}>Welcome back</Text>
              <Text style={styles.subtitle}>Sign in to find your next cup.</Text>
            </View>
            {error ? <MessageBanner message={error} /> : null}
            <FormField
              autoCapitalize="none"
              autoComplete="email"
              keyboardType="email-address"
              label="Email"
              onChangeText={setEmail}
              placeholder="you@example.com"
              returnKeyType="next"
              value={email}
            />
            <FormField
              autoCapitalize="none"
              autoComplete="current-password"
              label="Password"
              onChangeText={setPassword}
              onSubmitEditing={() => canSubmit && void handleLogin()}
              placeholder="At least 8 characters"
              returnKeyType="done"
              secureTextEntry
              value={password}
            />
            <PrimaryButton
              disabled={!canSubmit}
              isLoading={isSubmitting}
              label="Sign In"
              onPress={() => void handleLogin()}
            />
            <Text style={styles.accountPrompt}>
              New to Social Cup?{' '}
              <Link href="/(auth)/register" style={styles.link}>
                Create Account
              </Link>
            </Text>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { backgroundColor: colors.surface, flex: 1 },
  flex: { flex: 1 },
  scrollContent: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: spacing.xl,
  },
  brandBlock: { gap: spacing.sm, marginBottom: spacing.xxl },
  brand: {
    color: colors.primary,
    fontFamily: fonts.bold,
    fontSize: 34,
    letterSpacing: -1,
  },
  tagline: {
    color: colors.textMuted,
    fontFamily: fonts.regular,
    fontSize: 15,
  },
  card: {
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: radii.lg,
    borderWidth: 1,
    gap: spacing.lg,
    padding: spacing.xl,
  },
  headingBlock: { gap: spacing.xs },
  title: {
    color: colors.text,
    fontFamily: fonts.semibold,
    fontSize: 25,
  },
  subtitle: {
    color: colors.textMuted,
    fontFamily: fonts.regular,
    fontSize: 14,
  },
  accountPrompt: {
    color: colors.textMuted,
    fontFamily: fonts.regular,
    fontSize: 14,
    textAlign: 'center',
  },
  link: { color: colors.primary, fontFamily: fonts.semibold },
});
