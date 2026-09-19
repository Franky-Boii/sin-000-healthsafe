package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.model.WardRecord;
import com.opencsv.CSVReader;
import io.javalin.Javalin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class IngestionServiceApp {

    public static void main(String[] args) {
        List<WardRecord> wards = loadAndCleanWards();

        Javalin app = Javalin.create().start(7030);

        app.get("/health", ctx -> ctx.result("OK"));

        // Cleaned ward records for ward-service to consume.
        app.get("/wards", ctx -> ctx.json(wards));

        app.get("/wards/{wardId}", ctx -> {
            String wardId = ctx.pathParam("wardId").trim().toUpperCase();
            WardRecord match = wards.stream()
                    .filter(w -> w.wardId().equals(wardId))
                    .findFirst()
                    .orElse(null);

            if (match == null) {
                ctx.status(404).json(java.util.Map.of("error", "no ward with id " + wardId));
            } else {
                ctx.json(match);
            }
        });
    }

    private static List<WardRecord> loadAndCleanWards() {
        try (InputStream in = IngestionServiceApp.class.getResourceAsStream("/wards-outdated.csv")) {
            if (in == null) {
                throw new IllegalStateException("wards-outdated.csv not found on classpath");
            }
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                List<String[]> allRows = csvReader.readAll();
                List<String[]> dataRows = allRows.isEmpty() ? allRows : allRows.subList(1, allRows.size());
                return new WardCsvCleaner().clean(dataRows);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load and clean wards-outdated.csv", e);
        }
    }
}
