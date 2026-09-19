package co.wethinkcode.healthsafe;

import java.time.Instant;

/** The latest staffing update broadcast for a ward, as received off staffing-events-topic. */
public record StaffingUpdate(String wardId, String department, int alertLevel, int onCallDoctors, Instant receivedAt) {
}
