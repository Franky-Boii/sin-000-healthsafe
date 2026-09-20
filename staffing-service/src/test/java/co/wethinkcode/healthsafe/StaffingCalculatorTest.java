package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * StaffingCalculator is the pure domain logic behind staffing-service — no HTTP
 * calls to ward-service or alert-level-service happen here, so it's testable with
 * plain ints. StaffingServiceApp fetches the ward and the alert level and hands the
 * number to this class.
 */
class StaffingCalculatorTest {

    private final StaffingCalculator calculator = new StaffingCalculator();

    @Test
    @DisplayName("level 0 (no active emergency) still staffs the baseline on-call doctor")
    void levelZeroReturnsBaseline() {
        assertEquals(StaffingCalculator.BASE_DOCTORS, calculator.onCallDoctors(0));
    }

    @ParameterizedTest(name = "level {0} adds {0} doctor(s) on top of the baseline")
    @CsvSource({"1", "3", "8"})
    @DisplayName("each level above 0 adds one doctor per level")
    void higherLevelIncreasesStaffing(int level) {
        int expected = StaffingCalculator.BASE_DOCTORS + level;
        assertEquals(expected, calculator.onCallDoctors(level));
    }

    @Test
    @DisplayName("level 8 (full Code Blue) staffs the maximum on-call team")
    void levelEightReturnsMax() {
        assertEquals(StaffingCalculator.BASE_DOCTORS + 8, calculator.onCallDoctors(8));
    }

    @Test
    @DisplayName("a negative level is rejected rather than silently producing a nonsense schedule")
    void rejectsNegativeLevel() {
        assertThrows(IllegalArgumentException.class, () -> calculator.onCallDoctors(-1));
    }

    @Test
    @DisplayName("a level above the known max (8) is rejected rather than silently extrapolated")
    void rejectsLevelAboveMax() {
        assertThrows(IllegalArgumentException.class, () -> calculator.onCallDoctors(9));
    }
}
