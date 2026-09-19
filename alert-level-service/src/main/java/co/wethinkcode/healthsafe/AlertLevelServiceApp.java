package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import java.util.Map;

public class AlertLevelServiceApp {

    public static void main(String[] args) {
        AlertLevelStore store = new AlertLevelStore();

        Javalin app = Javalin.create().start(7032);

        app.get("/health", ctx -> ctx.result("OK"));

        // Read the current hospital-wide Emergency Status.
        app.get("/alert-level", ctx -> ctx.json(Map.of("level", store.currentLevel())));

        app.post("/alert-level", ctx -> {
            LevelUpdateRequest body = ctx.bodyAsClass(LevelUpdateRequest.class);
            try {
                store.setLevel(body.level());
                ctx.json(Map.of("level", store.currentLevel()));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("error", e.getMessage()));
            }
        });
    }

    /** Request body for POST /alert-level. */
    public record LevelUpdateRequest(int level) {
    }
}
