package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Fetches the current Emergency Status from alert-level-service's {@code GET /alert-level}. */
public class AlertLevelClient {

    private final String alertLevelServiceBaseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AlertLevelClient(String alertLevelServiceBaseUrl) {
        this.alertLevelServiceBaseUrl = alertLevelServiceBaseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public int fetchLevel() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(alertLevelServiceBaseUrl + "/alert-level"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "alert-level-service returned status " + response.statusCode() + " for GET /alert-level");
            }
            JsonNode json = objectMapper.readTree(response.body());
            return json.get("level").asInt();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException(
                    "failed to fetch alert level from " + alertLevelServiceBaseUrl, e);
        }
    }
}
