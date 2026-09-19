package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.model.WardDto;
import co.wethinkcode.healthsafe.mq.MqPublisher;
import io.javalin.Javalin;

import java.util.Map;
import java.util.Optional;

public class StaffingServiceApp {

    private static final String WARD_SERVICE_URL =
            System.getenv().getOrDefault("WARD_SERVICE_URL", "http://localhost:7031");
    private static final String ALERT_LEVEL_SERVICE_URL =
            System.getenv().getOrDefault("ALERT_LEVEL_SERVICE_URL", "http://localhost:7032");

    public static void main(String[] args) {
        WardServiceClient wardServiceClient = new WardServiceClient(WARD_SERVICE_URL);
        AlertLevelClient alertLevelClient = new AlertLevelClient(ALERT_LEVEL_SERVICE_URL);
        StaffingCalculator calculator = new StaffingCalculator();
        StaffingStore store = new StaffingStore();
        MqPublisher publisher = new MqPublisher();
        Runtime.getRuntime().addShutdownHook(new Thread(publisher::close));

        Javalin app = Javalin.create().start(7033);

        app.get("/health", ctx -> ctx.result("OK"));

        // Returns the last computed schedule for a ward, without recomputing —
        // 404 if nothing has been computed for it yet (call POST first).
        app.get("/schedule/{wardId}", ctx -> {
            String wardId = ctx.pathParam("wardId");
            store.currentSchedule(wardId)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json(Map.of(
                                    "error", "no schedule computed yet for ward " + wardId
                                            + " — POST to this endpoint to compute one")));
        });

        // Computes a fresh schedule: validates the ward via ward-service, reads
        // the current Emergency Status via alert-level-service, and calculates
        // the on-call count. If this differs from what was previously stored for
        // the ward, broadcasts the change on staffing-events-topic (stage 3) —
        // not on every call, only on an actual change, per RUBRIC.md.
        app.post("/schedule/{wardId}", ctx -> {
            String wardId = ctx.pathParam("wardId");
            Optional<WardDto> ward = wardServiceClient.fetchWard(wardId);

            if (ward.isEmpty()) {
                ctx.status(404).json(Map.of("error", "no ward with id " + wardId));
                return;
            }

            int alertLevel = alertLevelClient.fetchLevel();
            int onCallDoctors = calculator.onCallDoctors(alertLevel);
            Schedule schedule = new Schedule(ward.get().wardId(), ward.get().department(), alertLevel, onCallDoctors);

            boolean changed = store.recordSchedule(schedule);
            if (changed) {
                try {
                    publisher.publishScheduleChange(schedule);
                } catch (Exception mqError) {
                    System.err.println("MqPublisher: failed to publish schedule change for "
                            + schedule.wardId() + ": " + mqError.getMessage());
                }
            }

            ctx.json(schedule);
        });
    }
}
