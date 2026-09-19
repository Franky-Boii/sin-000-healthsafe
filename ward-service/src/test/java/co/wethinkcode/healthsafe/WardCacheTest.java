package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.model.WardDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WardCache is the pure in-memory query layer ward-service serves from — it never
 * makes an HTTP call itself, so these tests build it directly from hand-written
 * WardDto lists rather than hitting a running ingestion-service.
 */
class WardCacheTest {

    private final List<WardDto> sample = List.of(
            new WardDto("W-01", "East Wing", "Cardiology", 3, List.of()),
            new WardDto("W-02", "West Wing", "Paediatrics", null, List.of("missing/placeholder")),
            new WardDto("W-08", "", "Oncology", 4, List.of("missing wing for ward W-08"))
    );

    @Nested
    @DisplayName("all()")
    class All {

        @Test
        @DisplayName("returns every ward the cache was built with, in order")
        void returnsAllWards() {
            WardCache cache = new WardCache(sample);
            assertEquals(sample, cache.all());
        }
    }

    @Nested
    @DisplayName("byId()")
    class ById {

        @Test
        @DisplayName("finds a ward by exact id")
        void findsExactMatch() {
            WardCache cache = new WardCache(sample);
            Optional<WardDto> result = cache.byId("W-01");
            assertTrue(result.isPresent());
            assertEquals("Cardiology", result.get().department());
        }

        @Test
        @DisplayName("lookup is case-insensitive, since ingestion-service normalizes ids to uppercase")
        void lookupIsCaseInsensitive() {
            WardCache cache = new WardCache(sample);
            assertTrue(cache.byId("w-01").isPresent());
        }

        @Test
        @DisplayName("an unknown id returns empty, not null and not an exception")
        void unknownIdReturnsEmpty() {
            WardCache cache = new WardCache(sample);
            assertTrue(cache.byId("W-999").isEmpty());
        }
    }
}
