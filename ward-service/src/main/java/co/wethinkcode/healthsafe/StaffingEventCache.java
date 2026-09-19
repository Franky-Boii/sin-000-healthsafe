package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the latest staffing update seen per ward, as broadcast on
 * staffing-events-topic. Deliberately free of any JMS type — MqSubscriber owns the
 * broker connection and hands this class raw message bodies, which is what makes
 * this class testable without a running ActiveMQ broker (see
 * StaffingEventCacheTest).
 */
public class StaffingEventCache {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, StaffingUpdate> latestByWardId = new ConcurrentHashMap<>();

    /**
     * Parses a {@code {"wardId": ..., "department": ..., "alertLevel": ...,
     * "onCallDoctors": ...}} message body and records it. A malformed or
     * incomplete message is logged and ignored rather than thrown — one bad
     * message on the topic shouldn't take the subscriber down.
     */
    public void recordFromJson(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode wardIdNode = node.get("wardId");
            JsonNode onCallDoctorsNode = node.get("onCallDoctors");
            if (wardIdNode == null || onCallDoctorsNode == null) {
                System.err.println("StaffingEventCache: ignoring message missing wardId/onCallDoctors: " + json);
                return;
            }

            String wardId = normalize(wardIdNode.asText());
            if (wardId.isBlank()) {
                return;
            }

            JsonNode departmentNode = node.get("department");
            JsonNode alertLevelNode = node.get("alertLevel");
            String department = departmentNode == null ? "" : departmentNode.asText();
            int alertLevel = alertLevelNode == null ? 0 : alertLevelNode.asInt();

            latestByWardId.put(wardId,
                    new StaffingUpdate(wardId, department, alertLevel, onCallDoctorsNode.asInt(), Instant.now()));
        } catch (Exception e) {
            System.err.println("StaffingEventCache: ignoring unparseable message: " + json + " (" + e.getMessage() + ")");
        }
    }

    public Optional<StaffingUpdate> latestFor(String wardId) {
        return Optional.ofNullable(latestByWardId.get(normalize(wardId)));
    }

    private String normalize(String wardId) {
        return wardId == null ? "" : wardId.trim().toUpperCase();
    }
}
