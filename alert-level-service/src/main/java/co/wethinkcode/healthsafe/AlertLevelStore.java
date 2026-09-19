package co.wethinkcode.healthsafe;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the single hospital-wide Emergency Status (0-8, 8 = full Code Blue).
 * Deliberately free of Javalin/HTTP so it's unit tested directly (see
 * AlertLevelStoreTest). Unlike LogisticsConnect's per-hub DelayStageStore, there's
 * exactly one level here — the whole hospital shares one status.
 */
public class AlertLevelStore {

    private static final int MIN_LEVEL = 0;
    private static final int MAX_LEVEL = 8;

    private final AtomicInteger level = new AtomicInteger(MIN_LEVEL);

    public int currentLevel() {
        return level.get();
    }

    public void setLevel(int newLevel) {
        if (newLevel < MIN_LEVEL || newLevel > MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "level must be between " + MIN_LEVEL + " and " + MAX_LEVEL + ", got " + newLevel);
        }
        level.set(newLevel);
    }
}
