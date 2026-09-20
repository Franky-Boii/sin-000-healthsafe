package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StaffingStore holds the last computed schedule per ward and reports whether a
 * newly computed one actually changed anything — that "did it change" answer is
 * what POST /schedule/{wardId} uses to decide whether to publish to
 * staffing-events-topic (see RUBRIC.md: "publishes ... on schedule/status change",
 * not on every request).
 */
class StaffingStoreTest {

    @Nested
    @DisplayName("recordSchedule()")
    class RecordSchedule {

        @Test
        @DisplayName("the first schedule recorded for a ward counts as a change")
        void firstScheduleIsAChange() {
            StaffingStore store = new StaffingStore();
            boolean changed = store.recordSchedule(new Schedule("W-01", "Cardiology", 2, 3));
            assertTrue(changed);
        }

        @Test
        @DisplayName("recording an identical schedule again is not a change")
        void identicalScheduleIsNotAChange() {
            StaffingStore store = new StaffingStore();
            store.recordSchedule(new Schedule("W-01", "Cardiology", 2, 3));
            boolean changedAgain = store.recordSchedule(new Schedule("W-01", "Cardiology", 2, 3));
            assertFalse(changedAgain);
        }

        @Test
        @DisplayName("a different alert level for the same ward counts as a change")
        void differentLevelIsAChange() {
            StaffingStore store = new StaffingStore();
            store.recordSchedule(new Schedule("W-01", "Cardiology", 2, 3));
            boolean changed = store.recordSchedule(new Schedule("W-01", "Cardiology", 5, 6));
            assertTrue(changed);
        }

        @Test
        @DisplayName("lookup/storage is case-insensitive on ward id")
        void isCaseInsensitiveOnWardId() {
            StaffingStore store = new StaffingStore();
            store.recordSchedule(new Schedule("W-01", "Cardiology", 2, 3));
            assertTrue(store.currentSchedule("w-01").isPresent());
        }
    }

    @Nested
    @DisplayName("currentSchedule()")
    class CurrentSchedule {

        @Test
        @DisplayName("a ward with no recorded schedule returns empty, not null")
        void unknownWardReturnsEmpty() {
            StaffingStore store = new StaffingStore();
            assertTrue(store.currentSchedule("W-999").isEmpty());
        }

        @Test
        @DisplayName("returns the most recently recorded schedule for that ward")
        void returnsMostRecentSchedule() {
            StaffingStore store = new StaffingStore();
            store.recordSchedule(new Schedule("W-01", "Cardiology", 2, 3));
            store.recordSchedule(new Schedule("W-01", "Cardiology", 5, 6));

            Optional<Schedule> result = store.currentSchedule("W-01");
            assertTrue(result.isPresent());
            assertEquals(6, result.get().onCallDoctors());
        }
    }
}
