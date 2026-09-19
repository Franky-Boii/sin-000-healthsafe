package co.wethinkcode.healthsafe;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the last computed {@link Schedule} per ward, and reports whether a newly
 * computed one differs from what was there before. Deliberately free of Javalin/
 * HTTP/MQ so it's unit tested directly (see StaffingStoreTest).
 */
public class StaffingStore {

    private final Map<String, Schedule> schedulesByWardId = new ConcurrentHashMap<>();

    /** Records the schedule and returns true if it's new or different from what was stored. */
    public boolean recordSchedule(Schedule schedule) {
        Schedule previous = schedulesByWardId.put(normalize(schedule.wardId()), schedule);
        return previous == null || !Objects.equals(previous, schedule);
    }

    public Optional<Schedule> currentSchedule(String wardId) {
        return Optional.ofNullable(schedulesByWardId.get(normalize(wardId)));
    }

    private String normalize(String wardId) {
        return wardId == null ? "" : wardId.trim().toUpperCase();
    }
}
