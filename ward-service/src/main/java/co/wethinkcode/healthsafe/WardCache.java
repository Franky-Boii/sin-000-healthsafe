package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.model.WardDto;

import java.util.List;
import java.util.Optional;

/**
 * In-memory query layer over the ward data fetched from ingestion-service.
 * Populated once at startup by {@link IngestionClient} and served from for every
 * request.
 */
public class WardCache {

    private final List<WardDto> wards;

    public WardCache(List<WardDto> wards) {
        this.wards = List.copyOf(wards);
    }

    public List<WardDto> all() {
        return wards;
    }

    public Optional<WardDto> byId(String wardId) {
        String normalized = wardId == null ? "" : wardId.trim().toUpperCase();
        return wards.stream()
                .filter(w -> w.wardId() != null && w.wardId().equalsIgnoreCase(normalized))
                .findFirst();
    }
}
