import { Link, router } from 'expo-router';
import { useState } from 'react';
import {
  Alert,
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

export default function RegisterScreen() {
  const { register } = useAuth();
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleRegister() {
    setError(null);
    setIsSubmitting(true);
    try {
      await register({
        displayName: displayName.trim(),
        email: email.trim(),
        password,
      });
      Alert.alert('Account created', 'Sign in to continue to Social Cup.');
      router.replace('/(auth)/login');
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, 'Unable to create your account.'),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  const canSubmit =
    displayName.trim().length > 0 &&
    email.trim().length > 0 &&
    password.length >= 8;

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.flex}>
        <ScrollView
          contentContainerStyle={styles.scrollContent}
          keyboardShouldPersistTaps="handled">
          <View style={styles.headingBlock}>
            <Text style={styles.brand}>Social Cup</Text>
            <Text style={styles.title}>Create your account</Text>
            <Text style={styles.subtitle}>
              Start discovering independent coffee around Dallas.
            </Text>
          </View>

          <View style={styles.card}>
            {error ? <MessageBanner message={error} /> : null}
            <FormField
              autoCapitalize="words"
              autoComplete="name"
              label="Display name"
              maxLength={100}
              onChangeText={setDisplayName}
              placeholder="Nikhil"
              value={displayName}
            />
            <FormField
              autoCapitalize="none"
              autoComplete="email"
              keyboardType="email-address"
              label="Email"
              maxLength={255}
              onChangeText={setEmail}
              placeholder="you@example.com"
              value={email}
            />
            <FormField
              autoCapitalize="none"
              autoComplete="new-password"
              label="Password"
              maxLength={72}
              onChangeText={setPassword}
              onSubmitEditing={() => canSubmit && void handleRegister()}
              placeholder="At least 8 characters"
              returnKeyType="done"
              secureTextEntry
              value={password}
            />
            <PrimaryButton
              disabled={!canSubmit}
              isLoading={isSubmitting}
              label="Create Account"
              onPress={() => void handleRegister()}
            />
            <Text style={styles.accountPrompt}>
              Already have an account?{' '}
              <Link href="/(auth)/login" style={styles.link}>
                Sign In
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
  headingBlock: { gap: spacing.sm, marginBottom: spacing.xxl },
  brand: {
    color: colors.primary,
    fontFamily: fonts.bold,
    fontSize: 20,
  },
  title: {
    color: colors.text,
    fontFamily: fonts.semibold,
    fontSize: 29,
    lineHeight: 36,
  },
  subtitle: {
    color: colors.textMuted,
    fontFamily: fonts.regular,
    fontSize: 15,
    lineHeight: 23,
  },
  card: {
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: radii.lg,
    borderWidth: 1,
    gap: spacing.lg,
    padding: spacing.xl,
  },
  accountPrompt: {
    color: colors.textMuted,
    fontFamily: fonts.regular,
    fontSize: 14,
    textAlign: 'center',
  },
  link: { color: colors.primary, fontFamily: fonts.semibold },
});
