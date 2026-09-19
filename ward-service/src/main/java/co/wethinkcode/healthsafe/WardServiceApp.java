package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.model.WardDto;
import co.wethinkcode.healthsafe.mq.MqSubscriber;
import io.javalin.Javalin;

import java.util.Map;

public class WardServiceApp {

    private static final String INGESTION_SERVICE_URL =
            System.getenv().getOrDefault("INGESTION_SERVICE_URL", "http://localhost:7030");

    public static void main(String[] args) {
        WardCache cache = new WardCache(new IngestionClient(INGESTION_SERVICE_URL).fetchWards());

        // Stage 3: reacts to staffing-events-topic instead of polling anyone.
        StaffingEventCache staffingEventCache = new StaffingEventCache();
        MqSubscriber subscriber = new MqSubscriber(staffingEventCache);
        Runtime.getRuntime().addShutdownHook(new Thread(subscriber::close));

        Javalin app = Javalin.create().start(7031);

        app.get("/health", ctx -> ctx.result("OK"));

        // Provides lists of wards and departments, sourced from ingestion-service's
        // cleaned output and cached at startup.
        app.get("/wards", ctx -> ctx.json(cache.all()));

        app.get("/wards/{wardId}", ctx -> {
            String wardId = ctx.pathParam("wardId");
            cache.byId(wardId)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json(Map.of("error", "no ward with id " + wardId)));
        });

        // The latest staffing-events-topic broadcast received for this ward, if
        // any — proof this service is reacting asynchronously, not polling.
        app.get("/wards/{wardId}/staffing", ctx -> {
            String wardId = ctx.pathParam("wardId");
            staffingEventCache.latestFor(wardId)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json(Map.of(
                                    "error", "no staffing update received yet for ward " + wardId)));
        });
    }
}
