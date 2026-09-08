import {
  Poppins_400Regular,
  Poppins_500Medium,
  Poppins_600SemiBold,
  Poppins_700Bold,
  useFonts,
} from '@expo-google-fonts/poppins';
import { StripeProvider } from '@stripe/stripe-react-native';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { AuthProvider } from '@/src/auth/AuthContext';
import { colors } from '@/src/constants/theme';
import {
  isStripeConfigured,
  STRIPE_PUBLISHABLE_KEY,
  STRIPE_URL_SCHEME,
} from '@/src/constants/stripe';
import { RedemptionSessionProvider } from '@/src/redemption/RedemptionSessionContext';

void SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const [fontsLoaded, fontError] = useFonts({
    Poppins_400Regular,
    Poppins_500Medium,
    Poppins_600SemiBold,
    Poppins_700Bold,
  });

  useEffect(() => {
    if (fontError) {
      throw fontError;
    }
    if (fontsLoaded) {
      void SplashScreen.hideAsync();
    }
  }, [fontError, fontsLoaded]);

  if (!fontsLoaded) {
    return null;
  }

  const app = (
    <SafeAreaProvider>
      <AuthProvider>
        <RedemptionSessionProvider>
          <StatusBar style="dark" />
          <Stack
            screenOptions={{
              contentStyle: { backgroundColor: colors.surface },
              headerShown: false,
            }}>
            <Stack.Screen name="index" />
            <Stack.Screen name="(auth)" />
            <Stack.Screen name="(tabs)" />
            <Stack.Screen name="cafe/[id]" />
            <Stack.Screen name="redeem/[cafeId]" />
            <Stack.Screen name="redemption/[sessionId]" />
            <Stack.Screen name="membership" />
          </Stack>
        </RedemptionSessionProvider>
      </AuthProvider>
    </SafeAreaProvider>
  );

  if (!isStripeConfigured) {
    return app;
  }

  return (
    <StripeProvider
      publishableKey={STRIPE_PUBLISHABLE_KEY}
      urlScheme={STRIPE_URL_SCHEME}>
      {app}
    </StripeProvider>
  );
}
