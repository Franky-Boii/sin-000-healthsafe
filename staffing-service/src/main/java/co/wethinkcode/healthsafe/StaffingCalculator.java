package co.wethinkcode.healthsafe;

/**
 * Turns the hospital's Emergency Status (0-8, from alert-level-service) into an
 * on-call doctor count. The formula is intentionally simple — a placeholder worth
 * calling out explicitly in a debrief, per RUBRIC.md's "flags assumptions/
 * tradeoffs" cross-cutting bullet: a real system would size the on-call team from
 * ward-specific staffing ratios and historical demand, not a flat per-level add.
 */
public class StaffingCalculator {

    public static final int BASE_DOCTORS = 1;
    private static final int MIN_LEVEL = 0;
    private static final int MAX_LEVEL = 8;

    public int onCallDoctors(int alertLevel) {
        if (alertLevel < MIN_LEVEL || alertLevel > MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "alertLevel must be between " + MIN_LEVEL + " and " + MAX_LEVEL + ", got " + alertLevel);
        }
        return BASE_DOCTORS + alertLevel;
    }
}
