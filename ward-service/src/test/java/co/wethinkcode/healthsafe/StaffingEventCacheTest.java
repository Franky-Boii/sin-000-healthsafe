package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StaffingEventCache is the pure logic behind ward-service's stage-3 reaction to
 * staffing-events-topic — no JMS type appears here; MqSubscriber owns the broker
 * connection and hands this class raw message bodies, which is what makes it
 * testable without a running ActiveMQ broker.
 */
class StaffingEventCacheTest {

    @Nested
    @DisplayName("recordFromJson()")
    class RecordFromJson {

        @Test
        @DisplayName("a well-formed message updates the latest staffing update for that ward")
        void updatesFromValidMessage() {
            StaffingEventCache cache = new StaffingEventCache();
            cache.recordFromJson("{\"wardId\":\"W-01\",\"department\":\"Cardiology\",\"alertLevel\":3,\"onCallDoctors\":4}");

            var result = cache.latestFor("W-01");
            assertTrue(result.isPresent());
            assertEquals(4, result.get().onCallDoctors());
            assertEquals("Cardiology", result.get().department());
        }

        @Test
        @DisplayName("lookup is case-insensitive on ward id")
        void lookupIsCaseInsensitive() {
            StaffingEventCache cache = new StaffingEventCache();
            cache.recordFromJson("{\"wardId\":\"W-01\",\"department\":\"Cardiology\",\"alertLevel\":3,\"onCallDoctors\":4}");
            assertTrue(cache.latestFor("w-01").isPresent());
        }

        @Test
        @DisplayName("a later message for the same ward overwrites the earlier one")
        void lastMessageWins() {
            StaffingEventCache cache = new StaffingEventCache();
            cache.recordFromJson("{\"wardId\":\"W-01\",\"department\":\"Cardiology\",\"alertLevel\":2,\"onCallDoctors\":3}");
            cache.recordFromJson("{\"wardId\":\"W-01\",\"department\":\"Cardiology\",\"alertLevel\":6,\"onCallDoctors\":7}");

            assertEquals(7, cache.latestFor("W-01").get().onCallDoctors());
        }

        @Test
        @DisplayName("malformed JSON is ignored, not thrown")
        void malformedJsonDoesNotThrow() {
            StaffingEventCache cache = new StaffingEventCache();
            assertDoesNotThrow(() -> cache.recordFromJson("not json"));
        }

        @Test
        @DisplayName("a message missing onCallDoctors is ignored, not thrown")
        void missingFieldDoesNotThrow() {
            StaffingEventCache cache = new StaffingEventCache();
            assertDoesNotThrow(() -> cache.recordFromJson("{\"wardId\":\"W-01\"}"));
            assertTrue(cache.latestFor("W-01").isEmpty());
        }

        @Test
        @DisplayName("a malformed message doesn't corrupt a previously recorded update")
        void malformedMessageDoesNotCorruptExistingState() {
            StaffingEventCache cache = new StaffingEventCache();
            cache.recordFromJson("{\"wardId\":\"W-01\",\"department\":\"Cardiology\",\"alertLevel\":3,\"onCallDoctors\":4}");
            cache.recordFromJson("garbage");
            assertEquals(4, cache.latestFor("W-01").get().onCallDoctors());
        }
    }

    @Nested
    @DisplayName("latestFor()")
    class LatestFor {

        @Test
        @DisplayName("a ward with no message received yet returns empty")
        void unknownWardReturnsEmpty() {
            StaffingEventCache cache = new StaffingEventCache();
            assertTrue(cache.latestFor("W-999").isEmpty());
        }
    }
}
