package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.model.WardRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Turns raw rows from wards-outdated.csv into cleaned {@link WardRecord}s.
 * <p>
 * Deliberately separated from any CSV-parsing or Javalin/HTTP concern: this class
 * takes already-split rows (one {@code String[]} per data row, header excluded) so
 * the cleaning *rules* can be unit tested without touching a file or a server.
 * <p>
 * Expected column order per row, matching wards-outdated.csv:
 * {@code [ward_id, wing, department, beds_available]}
 * <p>
 * Duplicate strategy: same as LogisticsConnect's HubCsvCleaner — rows are keyed by
 * normalized ward id, first-seen order kept. An exact repeat collapses silently; a
 * conflicting repeat keeps the first occurrence and notes what was discarded.
 * <p>
 * beds_available strategy (a judgment call worth being able to defend):
 * blank/placeholder values and non-numeric values (including spelled-out numbers —
 * see the ingestion-service README's worked "five" example) become null with a
 * note; negative counts and unrealistically large values (a plausible mis-entered
 * year, e.g. 2023) are treated as invalid and become null; "full" is given a
 * semantic reading as zero beds available, since that's what the word means on a
 * ward chart, rather than being flagged as merely non-numeric.
 */
public class WardCsvCleaner {

    private static final Set<String> PLACEHOLDER_VALUES =
            Set.of("n/a", "na", "tbd", "unknown", "-", "nan", "");
    private static final int MAX_PLAUSIBLE_BEDS = 200;

    // Regional spelling variants seen in this dataset, mapped to the dominant
    // spelling already used by most rows.
    private static final Map<String, String> SPELLING_VARIANTS =
            Map.of("pediatrics", "Paediatrics");

    public List<WardRecord> clean(List<String[]> rawRows) {
        LinkedHashMap<String, WardRecord> byId = new LinkedHashMap<>();

        for (String[] row : rawRows) {
            WardRecord parsed = parseRow(row);
            WardRecord existing = byId.get(parsed.wardId());

            if (existing == null) {
                byId.put(parsed.wardId(), parsed);
            } else if (!sameCoreFields(existing, parsed)) {
                byId.put(parsed.wardId(), withConflictNote(existing, parsed));
            }
        }

        return new ArrayList<>(byId.values());
    }

    private WardRecord parseRow(String[] row) {
        String wardId = normalizeId(field(row, 0));
        List<String> notes = new ArrayList<>();

        String wing = collapseWhitespace(field(row, 1));
        if (wing.isBlank()) {
            notes.add("missing wing for ward " + wardId);
        } else {
            wing = titleCase(wing);
        }

        String department = titleCase(collapseWhitespace(field(row, 2)));
        String variant = SPELLING_VARIANTS.get(department.toLowerCase());
        if (variant != null) {
            department = variant;
        }

        Integer bedsAvailable = parseBedsAvailable(field(row, 3), wardId, notes);

        return new WardRecord(wardId, wing, department, bedsAvailable, notes);
    }

    private Integer parseBedsAvailable(String raw, String wardId, List<String> notes) {
        String value = raw == null ? "" : raw.trim();

        if (PLACEHOLDER_VALUES.contains(value.toLowerCase())) {
            notes.add("missing/placeholder bedsAvailable ('" + raw + "') for ward " + wardId);
            return null;
        }

        if (value.equalsIgnoreCase("full")) {
            return 0;
        }

        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            notes.add("bedsAvailable was non-numeric ('" + raw + "') for ward " + wardId + " — flagged for follow-up");
            return null;
        }

        if (parsed < 0) {
            notes.add("negative bedsAvailable (" + parsed + ") for ward " + wardId + " — invalid, flagged");
            return null;
        }

        if (parsed > MAX_PLAUSIBLE_BEDS) {
            notes.add("unrealistic bedsAvailable (" + parsed + ") for ward " + wardId
                    + " — likely a data entry error (e.g. a mistaken year), flagged");
            return null;
        }

        return parsed;
    }

    private boolean sameCoreFields(WardRecord a, WardRecord b) {
        return Objects.equals(a.wing(), b.wing())
                && Objects.equals(a.department(), b.department())
                && Objects.equals(a.bedsAvailable(), b.bedsAvailable());
    }

    private WardRecord withConflictNote(WardRecord existing, WardRecord conflicting) {
        List<String> mergedNotes = new ArrayList<>(existing.notes());
        mergedNotes.add("conflicting duplicate row for ward " + existing.wardId()
                + " discarded (wing='" + conflicting.wing()
                + "', department='" + conflicting.department()
                + "', bedsAvailable=" + conflicting.bedsAvailable() + "); kept first occurrence");
        return new WardRecord(existing.wardId(), existing.wing(), existing.department(),
                existing.bedsAvailable(), mergedNotes);
    }

    private String field(String[] row, int index) {
        return (row != null && index < row.length && row[index] != null) ? row[index] : "";
    }

    private String normalizeId(String raw) {
        return raw.trim().toUpperCase();
    }

    private String collapseWhitespace(String raw) {
        return raw.trim().replaceAll("\\s+", " ");
    }

    private String titleCase(String s) {
        if (s.isBlank()) {
            return s;
        }
        StringBuilder result = new StringBuilder();
        for (String word : s.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }
}
