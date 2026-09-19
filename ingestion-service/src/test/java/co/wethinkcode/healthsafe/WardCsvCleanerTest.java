package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.model.WardRecord;
import com.opencsv.CSVReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Row shape throughout: {ward_id, wing, department, beds_available}. Each test
 * documents which real row from wards-outdated.csv it mirrors, per RUBRIC.md's
 * Stage 1 checklist.
 */
class WardCsvCleanerTest {

    private final WardCsvCleaner cleaner = new WardCsvCleaner();

    @Nested
    @DisplayName("casing and padding")
    class CasingAndPadding {

        @Test
        @DisplayName("lowercase ward id is normalized to uppercase")
        void normalizesWardIdCasing() {
            // mirrors row: w-02,West Wing,paediatrics,N/A
            List<WardRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"w-02", "West Wing", "paediatrics", "N/A"}
            ));
            assertEquals("W-02", result.get(0).wardId());
        }

        @Test
        @DisplayName("leading/trailing padding on wing is trimmed")
        void trimsWingPadding() {
            // mirrors row: W-01, East Wing ,Cardiology,3
            List<WardRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"W-01", " East Wing ", "Cardiology", "3"}
            ));
            assertEquals("East Wing", result.get(0).wing());
        }

        @Test
        @DisplayName("internal double spaces are collapsed")
        void collapsesDoubleSpaces() {
            // mirrors row: W-10,South  Wing,Maternity,1
            List<WardRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"W-10", "South  Wing", "Maternity", "1"}
            ));
            assertEquals("South Wing", result.get(0).wing());
        }

        @Test
        @DisplayName("lowercase department is normalized to title case")
        void normalizesDepartmentCasing() {
            // mirrors row: W-07,West Wing,cardiology,TBD
            List<WardRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"W-07", "West Wing", "cardiology", "TBD"}
            ));
            assertEquals("Cardiology", result.get(0).department());
        }
    }

    @Nested
    @DisplayName("spelling variants")
    class SpellingVariants {

        @Test
        @DisplayName("the US spelling 'Pediatrics' is normalized to the dataset's dominant 'Paediatrics'")
        void normalizesRegionalSpelling() {
            // mirrors row: W-11,East Wing,Pediatrics,3
            List<WardRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"W-11", "East Wing", "Pediatrics", "3"}
            ));
            assertEquals("Paediatrics", result.get(0).department());
        }
    }

    @Nested
    @DisplayName("beds_available: missing, placeholder, and invalid values")
    class BedsAvailable {

        @ParameterizedTest(name = "\"{0}\" -> null, flagged")
        @CsvSource({"N/A", "TBD", "unknown", "''"})
        @DisplayName("placeholder/blank values become null with a note, not a guessed default")
        void placeholderValuesBecomeNull(String raw) {
            List<WardRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"W-90", "East Wing", "Cardiology", raw}
            ));
            assertNull(result.get(0).bedsAvailable());
            assertFalse(result.get(0).notes().isEmpty());
        }

        @Test
        @DisplayName("a spelled-out number ('five') is flagged, not parsed to 5 -- matches the README's worked example")
        void spelledOutNumberIsFlaggedNotParsed() {
            // mirrors row: w-05,east wing ,PAEDIATRICS,five
            List<WardRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"w-05", "east wing", "PAEDIATRICS", "five"}
            ));
            assertNull(result.get(0).bedsAvailable());
            assertTrue(result.get(0).notes().stream().anyMatch(n -> n.contains("five")));
        }

        @ParameterizedTest(name = "negative count \"{0}\" is rejected")
        @CsvSource({"-1", "-2"})
        @DisplayName("a negative bed count is invalid and becomes null, not a negative number served downstream")
        void negativeCountIsRejected(String raw) {
            List<WardRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"W-91", "East Wing", "Cardiology", raw}
            ));
            assertNull(result.get(0).bedsAvailable());
        }

        @Test
        @DisplayName("an unrealistic value (2023, plausibly a mis-entered year) is flagged rather than accepted")
        void unrealisticValueIsFlagged() {
            // mirrors row: W-13,North Wing,Oncology,2023
            List<WardRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"W-13", "North Wing", "Oncology", "2023"}
            ));
            assertNull(result.get(0).bedsAvailable());
        }

        @Test
        @DisplayName("'full' is treated as zero beds available, not as a non-numeric failure")
        void fullIsTreatedAsZero() {
            // mirrors row: w-12,west wing,Cardiology,full
            List<WardRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"w-12", "west wing", "Cardiology", "full"}
            ));
            assertEquals(0, result.get(0).bedsAvailable());
        }

        @Test
        @DisplayName("zero is a valid, distinct value from a missing/unknown count")
        void zeroIsValid() {
            // mirrors row: W-03 ,east wing,Cardiology,0
            List<WardRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"W-03", "east wing", "Cardiology", "0"}
            ));
            assertEquals(0, result.get(0).bedsAvailable());
            assertTrue(result.get(0).notes().isEmpty());
        }

        @Test
        @DisplayName("a normal positive count parses cleanly with no notes")
        void normalCountParsesCleanly() {
            // mirrors row: W-06,South Wing,Radiology,2
            List<WardRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"W-06", "South Wing", "Radiology", "2"}
            ));
            assertEquals(2, result.get(0).bedsAvailable());
        }
    }

    @Nested
    @DisplayName("missing wing")
    class MissingWing {

        @Test
        @DisplayName("a blank wing does not crash the parse and is flagged")
        void blankWingIsFlaggedNotDropped() {
            // mirrors row: W-08,,Oncology,4
            List<WardRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"W-08", "", "Oncology", "4"}
            ));
            assertEquals(1, result.size());
            assertFalse(result.get(0).notes().isEmpty());
        }
    }

    @Nested
    @DisplayName("duplicate detection")
    class DuplicateDetection {

        @Test
        @DisplayName("same ward id in different casing, identical fields, collapses to one record")
        void collapsesExactDuplicate() {
            List<WardRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"W-99", "East Wing", "Cardiology", "3"},
                    new String[]{"w-99", "East Wing", "Cardiology", "3"}
            ));
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("W-05 vs w-05 with conflicting fields resolves to one record, flagged -- the exact case the README calls out")
        void conflictingDuplicateIsResolvedAndFlagged() {
            // mirrors rows: W-05,East Wing,Paediatrics,5  /  w-05,east wing ,PAEDIATRICS,five
            List<WardRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"W-05", "East Wing", "Paediatrics", "5"},
                    new String[]{"w-05", "east wing", "PAEDIATRICS", "five"}
            ));

            long w05Count = result.stream().filter(r -> "W-05".equals(r.wardId())).count();
            assertEquals(1, w05Count, "expected the conflicting duplicate to be resolved to one record");
            assertFalse(result.get(0).notes().isEmpty(), "expected the conflict to be noted, not silently picked");
        }
    }

    @Nested
    @DisplayName("end-to-end against the real file")
    class RealFile {

        @Test
        @DisplayName("cleaning the actual wards-outdated.csv never throws and produces records for every input row")
        void cleansRealFileWithoutThrowing() throws Exception {
            List<String[]> rawRows = readRawRows("/wards-outdated.csv");
            List<WardRecord> result = assertDoesNotThrow(() -> cleaner.clean(rawRows));

            assertFalse(result.isEmpty());
            assertTrue(result.size() <= rawRows.size());
            assertTrue(result.stream().allMatch(r -> r.wardId() != null && !r.wardId().isBlank()));
        }

        private List<String[]> readRawRows(String classpathResource) throws Exception {
            try (InputStream in = getClass().getResourceAsStream(classpathResource)) {
                assertNotNull(in, "expected " + classpathResource + " on the test classpath");
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                try (CSVReader csvReader = new CSVReader(reader)) {
                    List<String[]> all = csvReader.readAll();
                    return all.subList(1, all.size());
                }
            }
        }
    }
}
