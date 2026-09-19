package co.wethinkcode.healthsafe.mq;

import co.wethinkcode.healthsafe.StaffingEventCache;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

/**
 * Subscribes to {@link MqConfig#TOPIC} (staffing-events-topic) and hands each
 * message's raw body to {@link StaffingEventCache}. This class owns the only
 * JMS-specific code on this side — the caching logic lives in StaffingEventCache,
 * unit tested without any of this.
 * <p>
 * Note: this is a *new* reaction for ward-service, not a replacement of an
 * existing synchronous call — ward-service never called staffing-service in
 * stage 2 (see the root README's imprecise wording on this point).
 */
public class MqSubscriber implements MessageListener, AutoCloseable {

    private final Connection connection;
    private final StaffingEventCache staffingEventCache;

    public MqSubscriber(StaffingEventCache staffingEventCache) {
        this.staffingEventCache = staffingEventCache;
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            connection = factory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(MqConfig.TOPIC);
            MessageConsumer consumer = session.createConsumer(topic);
            consumer.setMessageListener(this);
        } catch (JMSException e) {
            throw new IllegalStateException(
                    "failed to subscribe to ActiveMQ topic " + MqConfig.TOPIC
                            + " at " + MqConfig.BROKER_URL
                            + " — is `docker compose up -d` running in common/?", e);
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage textMessage) {
                staffingEventCache.recordFromJson(textMessage.getText());
            }
        } catch (JMSException e) {
            System.err.println("MqSubscriber: failed to read message body: " + e.getMessage());
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
