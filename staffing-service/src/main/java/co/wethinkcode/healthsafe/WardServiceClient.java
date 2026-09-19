package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.model.WardDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Validates a ward and fetches its details from ward-service's
 * {@code GET /wards/{wardId}}, per the root README's integration contract
 * ("staffing-service -> ward-service: validate the ward before scheduling").
 */
public class WardServiceClient {

    private final String wardServiceBaseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WardServiceClient(String wardServiceBaseUrl) {
        this.wardServiceBaseUrl = wardServiceBaseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /** Empty if ward-service returns 404 for this ward id; throws for any other failure. */
    public Optional<WardDto> fetchWard(String wardId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wardServiceBaseUrl + "/wards/" + wardId))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "ward-service returned status " + response.statusCode() + " for GET /wards/" + wardId);
            }
            return Optional.of(objectMapper.readValue(response.body(), WardDto.class));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException(
                    "failed to fetch ward " + wardId + " from ward-service at " + wardServiceBaseUrl, e);
        }
    }
}
