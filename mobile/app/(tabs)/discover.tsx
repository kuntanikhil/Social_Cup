import { useRouter } from 'expo-router';
import { useCallback, useEffect, useRef, useState, type PropsWithChildren } from 'react';
import {
  ActivityIndicator,
  Image,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { fetchDiscover, fetchNeighbourhoods } from '@/src/api/discoveryApi';
import { getApiErrorMessage } from '@/src/api/errors';
import { EmptyState } from '@/src/components/EmptyState';
import { MessageBanner } from '@/src/components/MessageBanner';
import { Screen } from '@/src/components/Screen';
import { colors, fonts, radii, spacing } from '@/src/constants/theme';
import type {
  DiscoverCafe,
  DiscoverResponse,
  FeaturedCafe,
  Neighbourhood,
  SignatureDrink,
} from '@/src/types/api';
import { resolveMediaUrl } from '@/src/utils/media';

const SEARCH_DELAY_MS = 350;

export default function DiscoverScreen() {
  const router = useRouter();
  const requestNumber = useRef(0);
  const [search, setSearch] = useState('');
  const [neighbourhoodId, setNeighbourhoodId] = useState<number | null>(null);
  const [neighbourhoods, setNeighbourhoods] = useState<Neighbourhood[]>([]);
  const [data, setData] = useState<DiscoverResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    async (refreshing = false) => {
      const currentRequest = ++requestNumber.current;
      refreshing ? setIsRefreshing(true) : setIsLoading(true);
      setError(null);
      try {
        const result = await fetchDiscover({
          ...(search.trim() ? { search: search.trim() } : {}),
          ...(neighbourhoodId ? { neighbourhoodId } : {}),
        });
        if (currentRequest === requestNumber.current) setData(result);
      } catch (requestError) {
        if (currentRequest === requestNumber.current) {
          setError(getApiErrorMessage(requestError, 'Unable to load cafes right now. Please try again.'));
        }
      } finally {
        if (currentRequest === requestNumber.current) {
          setIsLoading(false);
          setIsRefreshing(false);
        }
      }
    },
    [neighbourhoodId, search],
  );

  useEffect(() => {
    void fetchNeighbourhoods().then(setNeighbourhoods).catch(() => setNeighbourhoods([]));
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => void load(), SEARCH_DELAY_MS);
    return () => clearTimeout(timer);
  }, [load]);

  const openCafe = (id: number) => {
    router.push({ pathname: '/cafe/[id]', params: { id: String(id) } });
  };

  return (
    <Screen padded={false}>
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        refreshControl={<RefreshControl onRefresh={() => void load(true)} refreshing={isRefreshing} tintColor={colors.primary} />}>
        <View style={styles.header}>
          <Text style={styles.eyebrow}>Find your next cup</Text>
          <Text style={styles.title}>Discover</Text>
          <Text style={styles.subtitle}>Cafes and drinks picked around your Social Cup preferences.</Text>
        </View>

        <View style={styles.searchShell}>
          <Text style={styles.searchIcon}>⌕</Text>
          <TextInput
            accessibilityLabel="Search cafes"
            autoCapitalize="none"
            autoCorrect={false}
            onChangeText={setSearch}
            placeholder="Search cafes"
            placeholderTextColor={colors.textMuted}
            returnKeyType="search"
            style={styles.searchInput}
            value={search}
          />
          {search ? (
            <Pressable accessibilityLabel="Clear search" hitSlop={8} onPress={() => setSearch('')}>
              <Text style={styles.clear}>×</Text>
            </Pressable>
          ) : null}
        </View>

        <ScrollView contentContainerStyle={styles.filters} horizontal showsHorizontalScrollIndicator={false}>
          <FilterChip active={neighbourhoodId === null} label="All neighbourhoods" onPress={() => setNeighbourhoodId(null)} />
          {neighbourhoods.map((neighbourhood) => (
            <FilterChip
              active={neighbourhoodId === neighbourhood.id}
              key={neighbourhood.id}
              label={neighbourhood.name}
              onPress={() => setNeighbourhoodId(neighbourhood.id)}
            />
          ))}
        </ScrollView>

        {error ? <MessageBanner message={error} /> : null}
        {isLoading && !data ? (
          <View style={styles.loading}>
            <ActivityIndicator color={colors.primary} size="large" />
            <Text style={styles.loadingText}>Finding cafe favourites…</Text>
          </View>
        ) : null}

        {data ? (
          <>
            {data.featuredCafes.length > 0 ? (
              <Section title="Featured cafes">
                <ScrollView contentContainerStyle={styles.horizontalList} horizontal showsHorizontalScrollIndicator={false}>
                  {data.featuredCafes.map((cafe) => (
                    <FeaturedCafeCard cafe={cafe} key={cafe.id} onPress={() => openCafe(cafe.id)} />
                  ))}
                </ScrollView>
              </Section>
            ) : null}

            {data.signatureDrinks.length > 0 ? (
              <Section title="Signature drinks">
                <ScrollView contentContainerStyle={styles.horizontalList} horizontal showsHorizontalScrollIndicator={false}>
                  {data.signatureDrinks.map((drink) => (
                    <SignatureDrinkCard drink={drink} key={drink.id} onPress={() => openCafe(drink.cafe.id)} />
                  ))}
                </ScrollView>
              </Section>
            ) : null}

            <Section title="All cafes">
              {data.cafes.length ? (
                <View style={styles.cafeList}>
                  {data.cafes.map((cafe) => (
                    <CafeCard cafe={cafe} key={cafe.id} onPress={() => openCafe(cafe.id)} />
                  ))}
                </View>
              ) : (
                <EmptyState message="Try a different cafe name or neighbourhood." title="No cafes found" />
              )}
            </Section>
          </>
        ) : null}
      </ScrollView>
    </Screen>
  );
}

function Section({ children, title }: PropsWithChildren<{ title: string }>) {
  return <View style={styles.section}><Text style={styles.sectionTitle}>{title}</Text>{children}</View>;
}

function FilterChip({ active, label, onPress }: { active: boolean; label: string; onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" accessibilityState={{ selected: active }} onPress={onPress} style={({ pressed }) => [styles.filter, active && styles.filterActive, pressed && styles.pressed]}>
      <Text style={[styles.filterText, active && styles.filterTextActive]}>{label}</Text>
    </Pressable>
  );
}

function FeaturedCafeCard({ cafe, onPress }: { cafe: FeaturedCafe; onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.featuredCard, pressed && styles.pressed]}>
      <View style={styles.featuredBadge}><Text style={styles.featuredBadgeText}>Featured</Text></View>
      <Text numberOfLines={2} style={styles.featuredName}>{cafe.name}</Text>
      <Text style={styles.featuredNeighbourhood}>{cafe.neighbourhood}</Text>
      {cafe.perkLine ? <Text numberOfLines={2} style={styles.featuredPerk}>{cafe.perkLine}</Text> : null}
      <View style={styles.cardFooter}>
        <RatingText average={cafe.averageRating} isNew={cafe.newCafe} ratingCount={cafe.ratingCount} />
        <CreditText credits={cafe.minimumCreditPrice} prefix="From" />
      </View>
    </Pressable>
  );
}

function SignatureDrinkCard({ drink, onPress }: { drink: SignatureDrink; onPress: () => void }) {
  const photoUrl = resolveMediaUrl(drink.photoPath);
  return (
    <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.drinkCard, pressed && styles.pressed]}>
      {photoUrl ? <Image source={{ uri: photoUrl }} style={styles.drinkPhoto} /> : <View style={styles.drinkPlaceholder}><Text style={styles.drinkPlaceholderText}>SC</Text></View>}
      <View style={styles.signatureBadge}><Text style={styles.signatureBadgeText}>Signature</Text></View>
      <Text numberOfLines={2} style={styles.drinkName}>{drink.name}</Text>
      <Text numberOfLines={1} style={styles.drinkCafe}>{drink.cafe.name}</Text>
      <View style={styles.drinkCredits}><CreditText credits={drink.creditPrice} /></View>
    </Pressable>
  );
}

function CafeCard({ cafe, onPress }: { cafe: DiscoverCafe; onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.cafeCard, pressed && styles.pressed]}>
      <View style={styles.cafeInitial}><Text style={styles.cafeInitialText}>{cafe.name.charAt(0).toUpperCase()}</Text></View>
      <View style={styles.cafeCopy}>
        <View style={styles.cafeTitleRow}>
          <Text numberOfLines={1} style={styles.cafeName}>{cafe.name}</Text>
          {cafe.featured ? <Text style={styles.star}>★</Text> : null}
        </View>
        <Text numberOfLines={1} style={styles.cafeMeta}>
          {cafe.neighbourhood}{cafe.distanceKm !== null ? ` · ${cafe.distanceKm.toFixed(2)} km` : ''}
        </Text>
        {cafe.perkLine ? <Text numberOfLines={1} style={styles.cafePerk}>{cafe.perkLine}</Text> : null}
        <View style={styles.cardFooter}>
          <View style={styles.inlineMeta}>
            <RatingText average={cafe.averageRating} isNew={cafe.newCafe} ratingCount={cafe.ratingCount} />
            {cafe.preferenceMatch ? <Text style={styles.matchBadge}>Matches your taste</Text> : null}
          </View>
          <CreditText credits={cafe.minimumCreditPrice} prefix="From" />
        </View>
      </View>
    </Pressable>
  );
}

function RatingText({ average, isNew, ratingCount }: { average: number | null; isNew: boolean; ratingCount: number }) {
  return <Text style={styles.rating}>{isNew || average === null ? 'New' : `★ ${average.toFixed(1)} (${ratingCount})`}</Text>;
}

function CreditText({ credits, prefix }: { credits: number | null; prefix?: string }) {
  return <Text style={styles.credits}>{credits === null ? 'Menu coming soon' : `${prefix ? `${prefix} ` : ''}${credits} credits`}</Text>;
}

const styles = StyleSheet.create({
  content: { gap: spacing.xl, paddingBottom: spacing.xxxl },
  header: { gap: spacing.xs, paddingHorizontal: spacing.xl, paddingTop: spacing.lg },
  eyebrow: { color: colors.primary, fontFamily: fonts.semibold, fontSize: 12, letterSpacing: 1.1, textTransform: 'uppercase' },
  title: { color: colors.text, fontFamily: fonts.bold, fontSize: 32, lineHeight: 40 },
  subtitle: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 14, lineHeight: 22 },
  searchShell: { alignItems: 'center', backgroundColor: colors.card, borderColor: colors.border, borderRadius: radii.md, borderWidth: 1, flexDirection: 'row', marginHorizontal: spacing.xl, minHeight: 50, paddingHorizontal: spacing.lg },
  searchIcon: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 23 },
  searchInput: { color: colors.text, flex: 1, fontFamily: fonts.regular, fontSize: 15, paddingHorizontal: spacing.md },
  clear: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 25 },
  filters: { gap: spacing.sm, paddingHorizontal: spacing.xl },
  filter: { backgroundColor: colors.card, borderColor: colors.border, borderRadius: radii.pill, borderWidth: 1, paddingHorizontal: spacing.lg, paddingVertical: spacing.sm },
  filterActive: { backgroundColor: colors.primary, borderColor: colors.primary },
  filterText: { color: colors.textMuted, fontFamily: fonts.medium, fontSize: 12 },
  filterTextActive: { color: colors.white },
  pressed: { opacity: 0.75 },
  loading: { alignItems: 'center', gap: spacing.md, padding: spacing.xxxl },
  loadingText: { color: colors.textMuted, fontFamily: fonts.medium, fontSize: 13 },
  section: { gap: spacing.md, paddingHorizontal: spacing.xl },
  sectionTitle: { color: colors.text, fontFamily: fonts.semibold, fontSize: 20 },
  horizontalList: { gap: spacing.md, paddingRight: spacing.xl },
  featuredCard: { backgroundColor: colors.primary, borderRadius: radii.lg, minHeight: 205, padding: spacing.lg, width: 270 },
  featuredBadge: { alignSelf: 'flex-start', backgroundColor: '#F4DFBD', borderRadius: radii.pill, paddingHorizontal: spacing.md, paddingVertical: spacing.xs },
  featuredBadgeText: { color: colors.primary, fontFamily: fonts.semibold, fontSize: 10, textTransform: 'uppercase' },
  featuredName: { color: colors.white, fontFamily: fonts.semibold, fontSize: 21, lineHeight: 28, marginTop: spacing.lg },
  featuredNeighbourhood: { color: colors.secondary, fontFamily: fonts.medium, fontSize: 12, marginTop: spacing.xs },
  featuredPerk: { color: colors.secondary, flex: 1, fontFamily: fonts.regular, fontSize: 12, lineHeight: 18, marginTop: spacing.sm },
  cardFooter: { alignItems: 'center', flexDirection: 'row', gap: spacing.sm, justifyContent: 'space-between', marginTop: spacing.md },
  rating: { color: colors.warning, fontFamily: fonts.semibold, fontSize: 11 },
  credits: { color: colors.primary, fontFamily: fonts.semibold, fontSize: 11 },
  drinkCard: { backgroundColor: colors.card, borderColor: colors.border, borderRadius: radii.md, borderWidth: 1, overflow: 'hidden', paddingBottom: spacing.md, width: 180 },
  drinkPhoto: { backgroundColor: colors.secondary, height: 106, width: '100%' },
  drinkPlaceholder: { alignItems: 'center', backgroundColor: colors.secondary, height: 106, justifyContent: 'center' },
  drinkPlaceholderText: { color: colors.primary, fontFamily: fonts.bold, fontSize: 24 },
  signatureBadge: { alignSelf: 'flex-start', backgroundColor: '#F4DFBD', borderRadius: radii.pill, marginHorizontal: spacing.md, marginTop: spacing.md, paddingHorizontal: spacing.sm, paddingVertical: 3 },
  signatureBadgeText: { color: colors.primary, fontFamily: fonts.semibold, fontSize: 9, textTransform: 'uppercase' },
  drinkName: { color: colors.text, fontFamily: fonts.semibold, fontSize: 14, lineHeight: 20, marginHorizontal: spacing.md, marginTop: spacing.sm },
  drinkCafe: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 11, marginHorizontal: spacing.md, marginTop: spacing.xs },
  drinkCredits: { marginHorizontal: spacing.md, marginTop: spacing.sm },
  cafeList: { gap: spacing.md },
  cafeCard: { alignItems: 'center', backgroundColor: colors.card, borderColor: colors.border, borderRadius: radii.md, borderWidth: 1, flexDirection: 'row', gap: spacing.md, padding: spacing.md },
  cafeInitial: { alignItems: 'center', backgroundColor: colors.secondary, borderRadius: radii.md, height: 76, justifyContent: 'center', width: 76 },
  cafeInitialText: { color: colors.primary, fontFamily: fonts.bold, fontSize: 28 },
  cafeCopy: { flex: 1 },
  cafeTitleRow: { alignItems: 'center', flexDirection: 'row', gap: spacing.sm },
  cafeName: { color: colors.text, flex: 1, fontFamily: fonts.semibold, fontSize: 15 },
  star: { color: colors.warning, fontSize: 14 },
  cafeMeta: { color: colors.textMuted, fontFamily: fonts.medium, fontSize: 11, marginTop: 2 },
  cafePerk: { color: colors.textMuted, fontFamily: fonts.regular, fontSize: 11, marginTop: spacing.xs },
  inlineMeta: { alignItems: 'center', flexDirection: 'row', flexShrink: 1, gap: spacing.sm },
  matchBadge: { backgroundColor: '#E9F7F0', borderRadius: radii.pill, color: colors.success, fontFamily: fonts.semibold, fontSize: 9, overflow: 'hidden', paddingHorizontal: spacing.sm, paddingVertical: 3 },
});
