import { Redirect } from 'expo-router';

import { useAuth } from '@/src/auth/AuthContext';
import { LoadingScreen } from '@/src/components/LoadingScreen';

export default function IndexScreen() {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return <LoadingScreen />;
  }

  return <Redirect href={isAuthenticated ? '/(tabs)' : '/(auth)/login'} />;
}
