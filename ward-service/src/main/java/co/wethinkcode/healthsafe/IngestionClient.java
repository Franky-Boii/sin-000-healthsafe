package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.model.WardDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Fetches cleaned ward records from ingestion-service's {@code GET /wards}. Kept
 * separate from {@link WardCache} so the query logic stays unit-testable without a
 * running ingestion-service (see WardCacheTest).
 */
public class IngestionClient {

    private final String ingestionServiceBaseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IngestionClient(String ingestionServiceBaseUrl) {
        this.ingestionServiceBaseUrl = ingestionServiceBaseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public List<WardDto> fetchWards() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ingestionServiceBaseUrl + "/wards"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "ingestion-service returned status " + response.statusCode() + " for GET /wards");
            }
            return objectMapper.readValue(response.body(), objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, WardDto.class));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException(
                    "failed to fetch wards from ingestion-service at " + ingestionServiceBaseUrl, e);
        }
    }
}
