package co.wethinkcode.healthsafe.mq;

import co.wethinkcode.healthsafe.Schedule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.util.Map;

/**
 * Publishes a JSON message ({@code {"wardId": ..., "department": ..., "alertLevel":
 * ..., "onCallDoctors": ...}}) to {@link MqConfig#TOPIC} whenever
 * StaffingServiceApp's POST endpoint computes a schedule that differs from what was
 * previously stored (see StaffingStore.recordSchedule). One connection/session/
 * producer is opened at startup and reused.
 */
public class MqPublisher implements AutoCloseable {

    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MqPublisher() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(MqConfig.TOPIC);
            producer = session.createProducer(topic);
        } catch (JMSException e) {
            throw new IllegalStateException(
                    "failed to connect to ActiveMQ broker at " + MqConfig.BROKER_URL
                            + " — is `docker compose up -d` running in common/?", e);
        }
    }

    /**
     * Publishes a schedule change. Callers should treat a failure here as a
     * warning, not a reason to fail the underlying REST request — the state change
     * already succeeded in StaffingStore; a broker hiccup shouldn't break the
     * synchronous POST /schedule/{wardId} contract.
     */
    public void publishScheduleChange(Schedule schedule) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "wardId", schedule.wardId(),
                    "department", schedule.department(),
                    "alertLevel", schedule.alertLevel(),
                    "onCallDoctors", schedule.onCallDoctors()
            ));
            TextMessage message = session.createTextMessage(json);
            producer.send(message);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "failed to publish schedule change for ward " + schedule.wardId(), e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (JMSException e) {
            // best-effort shutdown
        }
    }
}
