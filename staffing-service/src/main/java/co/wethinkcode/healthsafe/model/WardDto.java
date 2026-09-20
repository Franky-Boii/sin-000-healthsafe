package co.wethinkcode.healthsafe.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Mirrors the JSON shape served by ward-service's {@code GET /wards/{wardId}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WardDto(String wardId, String wing, String department, Integer bedsAvailable, List<String> notes) {
}
