import { useCallback, useEffect, useRef, useState } from 'react';
import {
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getApiErrorMessage, getHttpStatus } from '@/src/api/errors';
import { getMyDrinkRating, setDrinkRating } from '@/src/api/ratingApi';
import { MessageBanner } from '@/src/components/MessageBanner';
import { PrimaryButton } from '@/src/components/PrimaryButton';
import { colors, fonts, radii, spacing } from '@/src/constants/theme';
import type { RatingResponse } from '@/src/types/api';

export type DrinkRatingTarget = {
  id: number;
  name: string;
  cafeName: string;
};

type DrinkRatingModalProps = {
  visible: boolean;
  drink: DrinkRatingTarget | null;
  onClose: () => void;
  onSaved: (rating: RatingResponse) => void;
};

export function DrinkRatingModal({
  visible,
  drink,
  onClose,
  onSaved,
}: DrinkRatingModalProps) {
  const requestId = useRef(0);
  const [stars, setStars] = useState(0);
  const [note, setNote] = useState('');
  const [isExisting, setIsExisting] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isReady, setIsReady] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadRating = useCallback(async () => {
    if (!drink) return;

    const currentRequest = ++requestId.current;
    setIsLoading(true);
    setIsReady(false);
    setError(null);
    setStars(0);
    setNote('');
    setIsExisting(false);

    try {
      const rating = await getMyDrinkRating(drink.id);
      if (currentRequest !== requestId.current) return;
      setStars(rating.stars);
      setNote(rating.note ?? '');
      setIsExisting(true);
      setIsReady(true);
    } catch (requestError) {
      if (currentRequest !== requestId.current) return;
      if (getHttpStatus(requestError) === 404) {
        setIsReady(true);
      } else {
        setError(
          getApiErrorMessage(
            requestError,
            'Unable to load your diary entry. Please try again.',
          ),
        );
      }
    } finally {
      if (currentRequest === requestId.current) setIsLoading(false);
    }
  }, [drink]);

  useEffect(() => {
    if (visible && drink) {
      void loadRating();
    } else {
      requestId.current += 1;
      setIsSaving(false);
      setError(null);
    }
  }, [drink, loadRating, visible]);

  const close = () => {
    if (!isSaving) onClose();
  };

  const save = async () => {
    if (!drink || !isReady || stars < 1 || isSaving) return;

    setIsSaving(true);
    setError(null);
    try {
      const saved = await setDrinkRating(drink.id, {
        stars,
        note: note.trim() || null,
      });
      onSaved(saved);
      onClose();
    } catch (requestError) {
      setError(
        getApiErrorMessage(
          requestError,
          'Unable to save your diary entry. Your note is still here so you can retry.',
        ),
      );
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Modal
      animationType="slide"
      onRequestClose={close}
      presentationStyle="overFullScreen"
      transparent
      visible={visible}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.overlay}>
        <SafeAreaView edges={['top', 'bottom']} style={styles.sheet}>
          <View style={styles.header}>
            <View style={styles.headingCopy}>
              <Text style={styles.eyebrow}>Your coffee story</Text>
              <Text style={styles.title}>
                {isExisting ? 'Edit Diary Entry' : 'Add to your Drink Diary'}
              </Text>
            </View>
            <Pressable
              accessibilityLabel="Close rating"
              accessibilityRole="button"
              disabled={isSaving}
              hitSlop={8}
              onPress={close}
              style={({ pressed }) => [
                styles.closeButton,
                pressed && styles.pressed,
              ]}>
              <Text style={styles.closeText}>×</Text>
            </Pressable>
          </View>

          <ScrollView
            contentContainerStyle={styles.content}
            keyboardShouldPersistTaps="handled">
            {drink ? (
              <View style={styles.drinkBlock}>
                <Text numberOfLines={2} style={styles.drinkName}>
                  {drink.name}
                </Text>
                <Text numberOfLines={1} style={styles.cafeName}>
                  {drink.cafeName}
                </Text>
              </View>
            ) : null}

            {error ? <MessageBanner message={error} /> : null}

            {!isReady ? (
              <View style={styles.loadActions}>
                <PrimaryButton
                  isLoading={isLoading}
                  label={isLoading ? 'Loading diary entry' : 'Try Again'}
                  onPress={() => void loadRating()}
                  variant="secondary"
                />
              </View>
            ) : (
              <>
                <View style={styles.fieldGroup}>
                  <Text style={styles.label}>How was it?</Text>
                  <View accessibilityRole="radiogroup" style={styles.stars}>
                    {[1, 2, 3, 4, 5].map((value) => (
                      <Pressable
                        accessibilityLabel={`${value} star${value === 1 ? '' : 's'}`}
                        accessibilityRole="radio"
                        accessibilityState={{ checked: stars === value }}
                        hitSlop={2}
                        key={value}
                        onPress={() => setStars(value)}
                        style={({ pressed }) => [
                          styles.starButton,
                          pressed && styles.pressed,
                        ]}>
                        <Text
                          style={[
                            styles.star,
                            value <= stars && styles.starSelected,
                          ]}>
                          {value <= stars ? '★' : '☆'}
                        </Text>
                      </Pressable>
                    ))}
                  </View>
                </View>

                <View style={styles.fieldGroup}>
                  <View style={styles.noteHeader}>
                    <Text style={styles.label}>Add a note (optional)</Text>
                    <Text style={styles.counter}>{note.length} / 140</Text>
                  </View>
                  <TextInput
                    accessibilityLabel="Drink diary note"
                    maxLength={140}
                    multiline
                    onChangeText={setNote}
                    placeholder="What stood out about this drink?"
                    placeholderTextColor={colors.textMuted}
                    selectionColor={colors.primary}
                    style={styles.noteInput}
                    textAlignVertical="top"
                    value={note}
                  />
                </View>

                <PrimaryButton
                  disabled={stars < 1}
                  isLoading={isSaving}
                  label={isExisting ? 'Save Changes' : 'Save to Diary'}
                  onPress={() => void save()}
                />
              </>
            )}
          </ScrollView>
        </SafeAreaView>
      </KeyboardAvoidingView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    backgroundColor: 'rgba(35, 24, 17, 0.46)',
    flex: 1,
    justifyContent: 'flex-end',
  },
  sheet: {
    backgroundColor: colors.surface,
    borderTopLeftRadius: radii.lg,
    borderTopRightRadius: radii.lg,
    maxHeight: '94%',
  },
  header: {
    alignItems: 'flex-start',
    borderBottomColor: colors.border,
    borderBottomWidth: 1,
    flexDirection: 'row',
    gap: spacing.md,
    justifyContent: 'space-between',
    padding: spacing.xl,
  },
  headingCopy: { flex: 1, gap: spacing.xs },
  eyebrow: {
    color: colors.primary,
    fontFamily: fonts.semibold,
    fontSize: 11,
    letterSpacing: 0.8,
    textTransform: 'uppercase',
  },
  title: {
    color: colors.text,
    fontFamily: fonts.bold,
    fontSize: 23,
    lineHeight: 30,
  },
  closeButton: {
    alignItems: 'center',
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: radii.pill,
    borderWidth: 1,
    height: 42,
    justifyContent: 'center',
    width: 42,
  },
  closeText: {
    color: colors.text,
    fontFamily: fonts.regular,
    fontSize: 27,
    lineHeight: 31,
  },
  content: { gap: spacing.xl, padding: spacing.xl, paddingBottom: spacing.xxxl },
  drinkBlock: {
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: radii.md,
    borderWidth: 1,
    gap: spacing.xs,
    padding: spacing.lg,
  },
  drinkName: {
    color: colors.text,
    fontFamily: fonts.semibold,
    fontSize: 17,
    lineHeight: 24,
  },
  cafeName: {
    color: colors.textMuted,
    fontFamily: fonts.regular,
    fontSize: 13,
  },
  loadActions: { minHeight: 150, justifyContent: 'center' },
  fieldGroup: { gap: spacing.sm },
  label: { color: colors.text, fontFamily: fonts.semibold, fontSize: 14 },
  stars: { flexDirection: 'row', justifyContent: 'space-between' },
  starButton: {
    alignItems: 'center',
    height: 52,
    justifyContent: 'center',
    width: 52,
  },
  star: { color: colors.border, fontSize: 38, lineHeight: 45 },
  starSelected: { color: colors.warning },
  noteHeader: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  counter: {
    color: colors.textMuted,
    fontFamily: fonts.medium,
    fontSize: 11,
    fontVariant: ['tabular-nums'],
  },
  noteInput: {
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: radii.md,
    borderWidth: 1,
    color: colors.text,
    fontFamily: fonts.regular,
    fontSize: 15,
    lineHeight: 22,
    minHeight: 126,
    padding: spacing.lg,
  },
  pressed: { opacity: 0.68 },
});
