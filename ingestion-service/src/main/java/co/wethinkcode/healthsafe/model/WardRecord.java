package co.wethinkcode.healthsafe.model;

import java.util.List;
import java.util.Objects;

/**
 * A single cleaned ward record, ready to be served over REST by IngestionServiceApp
 * and consumed by ward-service.
 * <p>
 * {@code bedsAvailable} is nullable: a non-numeric, negative, placeholder, or
 * unrealistic raw value ends up as {@code null} here (not defaulted to 0 or
 * guessed), with the reason captured in {@code notes} — see the ingestion-service
 * README's worked example (the "five" row) and "Known data issues" list.
 */
public final class WardRecord {

    private final String wardId;
    private final String wing;
    private final String department;
    private final Integer bedsAvailable;
    private final List<String> notes;

    public WardRecord(String wardId, String wing, String department, Integer bedsAvailable, List<String> notes) {
        this.wardId = wardId;
        this.wing = wing;
        this.department = department;
        this.bedsAvailable = bedsAvailable;
        this.notes = List.copyOf(notes);
    }

    public String wardId() {
        return wardId;
    }

    public String wing() {
        return wing;
    }

    public String department() {
        return department;
    }

    public Integer bedsAvailable() {
        return bedsAvailable;
    }

    public List<String> notes() {
        return notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WardRecord other)) return false;
        return Objects.equals(wardId, other.wardId)
                && Objects.equals(wing, other.wing)
                && Objects.equals(department, other.department)
                && Objects.equals(bedsAvailable, other.bedsAvailable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wardId, wing, department, bedsAvailable);
    }

    @Override
    public String toString() {
        return "WardRecord{wardId='%s', wing='%s', department='%s', bedsAvailable=%s, notes=%s}"
                .formatted(wardId, wing, department, bedsAvailable, notes);
    }
}
