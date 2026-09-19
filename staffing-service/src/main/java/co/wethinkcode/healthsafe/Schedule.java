package co.wethinkcode.healthsafe;

/** A computed on-call schedule for one ward, at a point-in-time Emergency Status. */
public record Schedule(String wardId, String department, int alertLevel, int onCallDoctors) {
}
