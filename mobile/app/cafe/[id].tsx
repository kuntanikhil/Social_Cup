import { Redirect, useLocalSearchParams, useRouter } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Image,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { fetchCafe } from '@/src/api/cafeApi';
import { getApiErrorMessage, getHttpStatus } from '@/src/api/errors';
import { useAuth } from '@/src/auth/AuthContext';
import { AppHeader } from '@/src/components/AppHeader';
import { EmptyState } from '@/src/components/EmptyState';
import { LoadingScreen } from '@/src/components/LoadingScreen';
import { MessageBanner } from '@/src/components/MessageBanner';
import { PrimaryButton } from '@/src/components/PrimaryButton';
import { Screen } from '@/src/components/Screen';
import { colors, fonts, radii, spacing } from '@/src/constants/theme';
import type { CafeDetail, CafeOpeningHours, Drink } from '@/src/types/api';
import { resolveMediaUrl } from '@/src/utils/media';

const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

export default function CafeDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const cafeId = Number(id);
  const router = useRouter();
  const { isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const [cafe, setCafe] = useState<CafeDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (refreshing = false) => {
    if (!Number.isInteger(cafeId) || cafeId <= 0) {
      setError('This cafe link is invalid.');
      setIsLoading(false);
      return;
    }
    refreshing ? setIsRefreshing(true) : setIsLoading(true);
    setError(null);
    try {
      setCafe(await fetchCafe(cafeId));
    } catch (requestError) {
      setError(
        getHttpStatus(requestError) === 404
          ? 'This cafe is unavailable right now.'
          : getApiErrorMessage(requestError, 'Unable to load this cafe.'),
      );
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, [cafeId]);

  useEffect(() => {
    if (isAuthenticated) void load();
  }, [isAuthenticated, load]);

  if (isAuthLoading) return <LoadingScreen />;
  if (!isAuthenticated) return <Redirect href="/(auth)/login" />;
  if (isLoading && !cafe) return <LoadingScreen label="Opening the cafe menu…" />;

  const photoUrls = cafe?.photos.map((photo) => resolveMediaUrl(photo.storagePath)).filter((url): url is string => url !== null) ?? [];

  return (
    <Screen padded={false}>
      <AppHeader title={cafe?.name ?? 'Cafe'} />
      <ScrollView
        contentContainerStyle={styles.content}
        refreshControl={<RefreshControl onRefresh={() => void load(true)} refreshing={isRefreshing} tintColor={colors.primary} />}>
        {error ? <MessageBanner message={error} /> : null}
        {cafe ? (
          <>
            {photoUrls.length ? (
              <ScrollView contentContainerStyle={styles.gallery} horizontal showsHorizontalScrollIndicator={false}>
                {photoUrls.map((url) => <Image key={url} source={{ uri: url }} style={styles.heroPhoto} />)}
              </ScrollView>
            ) : (
              <View style={styles.heroPlaceholder}><Text style={styles.heroInitial}>{cafe.name.charAt(0).toUpperCase()}</Text></View>
            )}

            <View style={styles.titleBlock}>
              <View style={styles.badgeRow}>
                {cafe.featured ? <Badge label="Featured" /> : null}
                <Badge label={cafe.newCafe || cafe.averageRating === null ? 'New cafe' : `★ ${cafe.averageRating.toFixed(1)} · ${cafe.ratingCount} ratings`} />
              </View>
              <Text style={styles.title}>{cafe.name}</Text>
              <Text style={styles.neighbourhood}>{cafe.neighbourhood.name}</Text>
              {cafe.perkLine ? <Text style={styles.perk}>{cafe.perkLine}</Text> : null}
            </View>

            <View style={styles.infoCard}>
              <InfoRow label="Address" value={cafe.address} />
              {cafe.openingHours.length ? <OpeningHours hours={cafe.openingHours} /> : null}
            </View>

            <View style={styles.menuSection}>
              <Text style={styles.sectionTitle}>Menu</Text>
              {cafe.drinks.length ? cafe.drinks.map((drink) => <DrinkCard drink={drink} key={drink.id} />) : (
                <EmptyState message="This cafe has not published active drinks yet." title="Menu coming soon" />
              )}
            </View>
          </>
        ) : null}
      </ScrollView>
      {cafe && cafe.drinks.length ? (
        <View style={styles.actionBar}>
          <PrimaryButton
            label="Redeem Here"
            onPress={() => router.push({ pathname: '/redeem/[cafeId]', params: { cafeId: String(cafe.id) } })}
          />
        </View>
      ) : null}
    </Screen>
  );
}

function Badge({ label }: { label: string }) {
  return <Text style={styles.badge}>{label}</Text>;
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return <View style={styles.infoRow}><Text style={styles.infoLabel}>{label}</Text><Text style={styles.infoValue}>{value}</Text></View>;
}

function OpeningHours({ hours }: { hours: CafeOpeningHours[] }) {
  return (
    <View style={styles.hoursBlock}>
      <Text style={styles.infoLabel}>Opening hours</Text>
      {hours.map((item) => (
        <View key={item.id} style={styles.hoursRow}>
          <Text style={styles.day}>{DAYS[item.dayOfWeek - 1] ?? `Day ${item.dayOfWeek}`}</Text>
          <Text style={styles.time}>{item.closed ? 'Closed' : `${formatTime(item.opensAt)} – ${formatTime(item.closesAt)}`}</Text>
        </View>
      ))}
    </View>
  );
}

function DrinkCard({ drink }: { drink: Drink }) {
  const photoUrl = resolveMediaUrl(drink.photoPath);
  return (
    <View style={styles.drinkCard}>
      {photoUrl ? <Image source={{ uri: photoUrl }} style={styles.drinkPhoto} /> : <View style={styles.drinkPhotoPlaceholder}><Text style={styles.drinkPhotoText}>SC</Text></View>}
      <View style={styles.drinkCopy}>
        <View style={styles.drinkTitleRow}>
          <Text numberOfLines={2} style={styles.drinkName}>{drink.name}</Text>
          {drink.signature ? <Text style={styles.signature}>Signature</Text> : null}
        </View>
        <Text style={styles.drinkType}>{drink.type.split('_').join(' ')}</Text>
        {drink.description ? <Text numberOfLines={2} style={styles.drinkDescription}>{drink.description}</Text> : null}
        <View style={styles.priceRow}>
          <Text style={styles.retailPrice}>${Number(drink.retailPrice).toFixed(2)}</Text>
          <Text style={styles.creditPrice}>{drink.creditPrice} credits</Text>
        </View>
      </View>
    </View>
  );
}

function formatTime(time: string | null): string {
  if (!time) return '—';
  const [hourText, minute = '00'] = time.split(':');
  const hour = Number(hourText);
  if (!Number.isFinite(hour)) return time;
  const suffix = hour >= 12 ? 'PM' : 'AM';
  const displayHour = hour % 12 || 12;
  return `${displayHour}:${minute} ${suffix}`;
}

const styles = StyleSheet.create({
  content: { gap: spacing.xl, paddingBottom: spacing.xxxl, paddingHorizontal: spacing.xl },
  gallery: { gap: spacing.md, paddingRight: spacing.xl },
  heroPhoto: { backgroundColor: colors.secondary, borderRadius: radii.lg, height: 230, width: 310 },
  heroPlaceholder: { alignItems: 'center', backgroundColor: colors.secondary, borderRadius: radii.lg, height: 220, justifyContent: 'center' },
  heroInitial: { color: colors.primary, fontFamily: fonts.bold, fontSize: 64 },
  titleBlock: { gap: spacing.xs },
  badgeRow: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  badge: { backgroundColor: '#F4DFBD', borderRadius: radii.pill, color: colors.primary, fontFamily: fonts.semibold, fontSize: 10, overflow: 'hidden', paddingHorizontal: spacing.md, paddingVertical: spacing.xs },
  title: { color: colors.text, fontFamily: fonts.bold, fontSize: 28, lineHeight: 36 },
  neighbourhood: { color: colors.primary, fontFamily: fonts.semibold, fontSize: 13 },
  perk: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 15, lineHeight: 23, marginTop: spacing.sm },
  infoCard: { backgroundColor: colors.card, borderColor: colors.border, borderRadius: radii.md, borderWidth: 1, gap: spacing.lg, padding: spacing.lg },
  infoRow: { gap: spacing.xs },
  infoLabel: { color: colors.textMuted, fontFamily: fonts.semibold, fontSize: 11, letterSpacing: 0.7, textTransform: 'uppercase' },
  infoValue: { color: colors.text, fontFamily: fonts.medium, fontSize: 14, lineHeight: 21 },
  hoursBlock: { borderTopColor: colors.border, borderTopWidth: 1, gap: spacing.sm, paddingTop: spacing.lg },
  hoursRow: { flexDirection: 'row', justifyContent: 'space-between' },
  day: { color: colors.text, fontFamily: fonts.medium, fontSize: 12 },
  time: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 12 },
  menuSection: { gap: spacing.md },
  sectionTitle: { color: colors.text, fontFamily: fonts.semibold, fontSize: 21 },
  drinkCard: { backgroundColor: colors.card, borderColor: colors.border, borderRadius: radii.md, borderWidth: 1, flexDirection: 'row', gap: spacing.md, padding: spacing.md },
  drinkPhoto: { backgroundColor: colors.secondary, borderRadius: radii.sm, height: 92, width: 82 },
  drinkPhotoPlaceholder: { alignItems: 'center', backgroundColor: colors.secondary, borderRadius: radii.sm, height: 92, justifyContent: 'center', width: 82 },
  drinkPhotoText: { color: colors.primary, fontFamily: fonts.bold, fontSize: 20 },
  drinkCopy: { flex: 1 },
  drinkTitleRow: { alignItems: 'flex-start', flexDirection: 'row', gap: spacing.sm },
  drinkName: { color: colors.text, flex: 1, fontFamily: fonts.semibold, fontSize: 14, lineHeight: 20 },
  signature: { backgroundColor: '#F4DFBD', borderRadius: radii.pill, color: colors.primary, fontFamily: fonts.semibold, fontSize: 8, overflow: 'hidden', paddingHorizontal: spacing.sm, paddingVertical: 3 },
  drinkType: { color: colors.primary, fontFamily: fonts.medium, fontSize: 10, marginTop: 2, textTransform: 'capitalize' },
  drinkDescription: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 11, lineHeight: 17, marginTop: spacing.xs },
  priceRow: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between', marginTop: spacing.sm },
  retailPrice: { color: colors.textMuted, fontFamily: fonts.medium, fontSize: 11 },
  creditPrice: { color: colors.primary, fontFamily: fonts.semibold, fontSize: 12 },
  actionBar: { backgroundColor: colors.surface, borderTopColor: colors.border, borderTopWidth: 1, padding: spacing.lg, paddingBottom: spacing.xl },
});
