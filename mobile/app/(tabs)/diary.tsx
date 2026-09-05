import { Screen } from '@/src/components/Screen';
import { SectionPlaceholder } from '@/src/components/SectionPlaceholder';

export default function DiaryScreen() {
  return (
    <Screen>
      <SectionPlaceholder
        body="Your rated drinks will live here when the ratings interface is added."
        eyebrow="Your coffee story"
        title="Drink diary"
      />
    </Screen>
  );
}
