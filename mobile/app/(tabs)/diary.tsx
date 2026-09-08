import { useFocusEffect, useRouter } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  FlatList,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { getApiErrorMessage } from '@/src/api/errors';
import { getDrinkDiary } from '@/src/api/ratingApi';
import { useAuth } from '@/src/auth/AuthContext';
import {
  DrinkRatingModal,
  type DrinkRatingTarget,
} from '@/src/components/DrinkRatingModal';
import { LoadingScreen } from '@/src/components/LoadingScreen';
import { MessageBanner } from '@/src/components/MessageBanner';
import { PrimaryButton } from '@/src/components/PrimaryButton';
import { Screen } from '@/src/components/Screen';
import { colors, fonts, radii, spacing } from '@/src/constants/theme';
import type { DrinkDiaryEntry } from '@/src/types/api';

export default function DiaryScreen() {
  const router = useRouter();
  const { user } = useAuth();
  const entriesRef = useRef<DrinkDiaryEntry[] | null>(null);
  const requestId = useRef(0);
  const [entries, setEntries] = useState<DrinkDiaryEntry[] | null>(null);
  const [selectedDrink, setSelectedDrink] = useState<DrinkRatingTarget | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    requestId.current += 1;
    entriesRef.current = null;
    setEntries(null);
    setError(null);
    setSuccess(null);
    setSelectedDrink(null);
    setIsLoading(true);
  }, [user?.id]);

  const load = useCallback(async (refreshing = false) => {
    const currentRequest = ++requestId.current;
    if (refreshing) {
      setIsRefreshing(true);
    } else if (entriesRef.current === null) {
      setIsLoading(true);
    }
    setError(null);

    try {
      const nextEntries = await getDrinkDiary();
      if (currentRequest !== requestId.current) return;
      entriesRef.current = nextEntries;
      setEntries(nextEntries);
    } catch (requestError) {
      if (currentRequest !== requestId.current) return;
      setError(
        getApiErrorMessage(
          requestError,
          'Unable to load your Drink Diary right now. Please try again.',
        ),
      );
    } finally {
      if (currentRequest === requestId.current) {
        setIsLoading(false);
        setIsRefreshing(false);
      }
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      if (user) void load();
    }, [load, user]),
  );

  const openEntry = (entry: DrinkDiaryEntry) => {
    setSuccess(null);
    setSelectedDrink({
      id: entry.drinkId,
      name: entry.drinkName,
      cafeName: entry.cafeName,
    });
  };

  if (isLoading && entries === null) {
    return <LoadingScreen label="Opening your Drink Diary…" />;
  }

  return (
    <Screen padded={false}>
      <FlatList
        contentContainerStyle={styles.content}
        data={entries ?? []}
        keyExtractor={(entry) => String(entry.ratingId)}
        ListEmptyComponent={
          entries !== null ? (
            <DiaryEmptyState
              onDiscover={() => router.push('/(tabs)/discover')}
            />
          ) : null
        }
        ListHeaderComponent={
          <View style={styles.headerContent}>
            <View style={styles.heading}>
              <Text style={styles.eyebrow}>Your coffee story</Text>
              <Text style={styles.title}>Drink Diary</Text>
              <Text style={styles.subtitle}>
                {entries?.length
                  ? `${entries.length} ${entries.length === 1 ? 'drink' : 'drinks'} tasted`
                  : 'Save the cups worth remembering.'}
              </Text>
            </View>
            {success ? <MessageBanner message={success} tone="success" /> : null}
            {error ? (
              <View style={styles.errorBlock}>
                <MessageBanner message={error} />
                <PrimaryButton
                  label="Retry"
                  onPress={() => void load()}
                  variant="secondary"
                />
              </View>
            ) : null}
          </View>
        }
        refreshControl={
          <RefreshControl
            onRefresh={() => void load(true)}
            refreshing={isRefreshing}
            tintColor={colors.primary}
          />
        }
        renderItem={({ item }) => (
          <DiaryCard entry={item} onPress={() => openEntry(item)} />
        )}
        showsVerticalScrollIndicator={false}
      />

      <DrinkRatingModal
        drink={selectedDrink}
        onClose={() => setSelectedDrink(null)}
        onSaved={() => {
          setSuccess('Your Drink Diary has been updated.');
          setSelectedDrink(null);
          void load();
        }}
        visible={selectedDrink !== null}
      />
    </Screen>
  );
}

function DiaryCard({
  entry,
  onPress,
}: {
  entry: DrinkDiaryEntry;
  onPress: () => void;
}) {
  return (
    <Pressable
      accessibilityHint="Opens this diary entry for editing"
      accessibilityLabel={`${entry.drinkName}, ${entry.stars} out of 5 stars`}
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [styles.card, pressed && styles.pressed]}>
      <Text accessibilityElementsHidden style={styles.stars}>
        {'★'.repeat(entry.stars)}{'☆'.repeat(5 - entry.stars)}
      </Text>
      <Text numberOfLines={2} style={styles.drinkName}>
        {entry.drinkName}
      </Text>
      <Text numberOfLines={1} style={styles.cafeName}>
        {entry.cafeName}
      </Text>
      {entry.note ? <Text style={styles.note}>“{entry.note}”</Text> : null}
      <View style={styles.cardFooter}>
        <Text style={styles.date}>{formatDiaryDate(entry.ratedAt)}</Text>
        <Text style={styles.editLabel}>Edit entry</Text>
      </View>
    </Pressable>
  );
}

function DiaryEmptyState({ onDiscover }: { onDiscover: () => void }) {
  return (
    <View style={styles.emptyCard}>
      <Text style={styles.emptyIcon}>☕</Text>
      <Text style={styles.emptyTitle}>Your Drink Diary is empty</Text>
      <Text style={styles.emptyMessage}>
        Rate a drink you&apos;ve tried and it will appear here.
      </Text>
      <View style={styles.emptyAction}>
        <PrimaryButton label="Discover drinks" onPress={onDiscover} />
      </View>
    </View>
  );
}

function formatDiaryDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(date);
}

const styles = StyleSheet.create({
  content: {
    gap: spacing.md,
    paddingBottom: spacing.xxxl,
    paddingHorizontal: spacing.xl,
  },
  headerContent: { gap: spacing.lg, paddingBottom: spacing.xl },
  heading: { gap: spacing.xs, paddingTop: spacing.lg },
  eyebrow: {
    color: colors.primary,
    fontFamily: fonts.semibold,
    fontSize: 12,
    letterSpacing: 1,
    textTransform: 'uppercase',
  },
  title: {
    color: colors.text,
    fontFamily: fonts.bold,
    fontSize: 32,
    lineHeight: 40,
  },
  subtitle: {
    color: colors.textMuted,
    fontFamily: fonts.regular,
    fontSize: 14,
    lineHeight: 22,
  },
  errorBlock: { gap: spacing.md },
  card: {
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: radii.md,
    borderWidth: 1,
    gap: spacing.xs,
    padding: spacing.lg,
  },
  pressed: { opacity: 0.72 },
  stars: {
    color: colors.warning,
    fontFamily: fonts.regular,
    fontSize: 20,
    letterSpacing: 2,
  },
  drinkName: {
    color: colors.text,
    fontFamily: fonts.semibold,
    fontSize: 17,
    lineHeight: 24,
    marginTop: spacing.xs,
  },
  cafeName: {
    color: colors.primary,
    fontFamily: fonts.medium,
    fontSize: 12,
  },
  note: {
    color: colors.textMuted,
    fontFamily: fonts.regular,
    fontSize: 14,
    lineHeight: 22,
    marginTop: spacing.sm,
  },
  cardFooter: {
    alignItems: 'center',
    borderTopColor: colors.border,
    borderTopWidth: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: spacing.md,
    paddingTop: spacing.md,
  },
  date: {
    color: colors.textMuted,
    fontFamily: fonts.medium,
    fontSize: 11,
  },
  editLabel: {
    color: colors.primary,
    fontFamily: fonts.semibold,
    fontSize: 11,
  },
  emptyCard: {
    alignItems: 'center',
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: radii.lg,
    borderWidth: 1,
    gap: spacing.sm,
    padding: spacing.xl,
  },
  emptyIcon: { fontSize: 36 },
  emptyTitle: {
    color: colors.text,
    fontFamily: fonts.semibold,
    fontSize: 18,
    marginTop: spacing.xs,
    textAlign: 'center',
  },
  emptyMessage: {
    color: colors.textMuted,
    fontFamily: fonts.regular,
    fontSize: 13,
    lineHeight: 20,
    textAlign: 'center',
  },
  emptyAction: { marginTop: spacing.md, width: '100%' },
});
