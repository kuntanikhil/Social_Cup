import { Redirect, Stack } from 'expo-router';

import { useAuth } from '@/src/auth/AuthContext';
import { LoadingScreen } from '@/src/components/LoadingScreen';

export default function AuthLayout() {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return <LoadingScreen />;
  }
  if (isAuthenticated) {
    return <Redirect href="/(tabs)" />;
  }

  return <Stack screenOptions={{ headerShown: false }} />;
}
