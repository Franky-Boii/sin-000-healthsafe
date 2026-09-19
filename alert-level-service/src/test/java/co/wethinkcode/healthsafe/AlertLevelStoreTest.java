package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AlertLevelStore is the pure in-memory logic behind alert-level-service's REST
 * endpoints — no Javalin, no HTTP, so it's tested directly here.
 */
class AlertLevelStoreTest {

    @Test
    @DisplayName("a fresh store defaults to level 0 (no active emergency)")
    void defaultsToZero() {
        AlertLevelStore store = new AlertLevelStore();
        assertEquals(0, store.currentLevel());
    }

    @Test
    @DisplayName("a valid level (0-8) is recorded")
    void recordsValidLevel() {
        AlertLevelStore store = new AlertLevelStore();
        store.setLevel(5);
        assertEquals(5, store.currentLevel());
    }

    @Test
    @DisplayName("the boundaries (0 and 8, full Code Blue) are accepted")
    void acceptsBoundaryValues() {
        AlertLevelStore store = new AlertLevelStore();
        store.setLevel(8);
        assertEquals(8, store.currentLevel());
        store.setLevel(0);
        assertEquals(0, store.currentLevel());
    }

    @Test
    @DisplayName("a negative level is rejected, not silently clamped")
    void rejectsNegativeLevel() {
        AlertLevelStore store = new AlertLevelStore();
        assertThrows(IllegalArgumentException.class, () -> store.setLevel(-1));
    }

    @Test
    @DisplayName("a level above 8 is rejected, not silently clamped")
    void rejectsTooHighLevel() {
        AlertLevelStore store = new AlertLevelStore();
        assertThrows(IllegalArgumentException.class, () -> store.setLevel(9));
    }

    @Test
    @DisplayName("a rejected update leaves the previous level unchanged")
    void rejectedUpdateLeavesLevelUnchanged() {
        AlertLevelStore store = new AlertLevelStore();
        store.setLevel(4);
        assertThrows(IllegalArgumentException.class, () -> store.setLevel(20));
        assertEquals(4, store.currentLevel());
    }

    @Test
    @DisplayName("setting a new level overwrites the previous one")
    void overwritesPreviousLevel() {
        AlertLevelStore store = new AlertLevelStore();
        store.setLevel(2);
        store.setLevel(7);
        assertEquals(7, store.currentLevel());
    }
}
