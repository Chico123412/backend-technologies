package com.autoservice.messaging;

import com.autoservice.config.RabbitConfig;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "catalog.events.relay-enabled", havingValue = "true", matchIfMissing = true)
public class CatalogEventRelay {
    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;

    public CatalogEventRelay(JdbcTemplate jdbc, RabbitTemplate rabbit) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
    }

    @Scheduled(fixedDelayString = "${catalog.events.poll-ms:2000}")
    public void publishPending() {
        var events = jdbc.query("""
                SELECT event_id, action, service_id, service_name FROM catalog_event_outbox
                WHERE published = false ORDER BY created_at, event_id LIMIT 50
                """, (row, index) -> new ServiceEvent(row.getString(1), row.getString(2),
                row.getLong(3), row.getString(4)));
        for (var event : events) {
            try {
                var confirmation = new CorrelationData(event.eventId());
                rabbit.convertAndSend(RabbitConfig.EXCHANGE, "service.changed", event, message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                }, confirmation);
                var confirm = confirmation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.isAck() || confirmation.getReturned() != null) {
                    throw new IllegalStateException("RabbitMQ не підтвердив доставку в чергу");
                }
                jdbc.update("UPDATE catalog_event_outbox SET published = true WHERE event_id = ?", event.eventId());
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception error) {
                LoggerFactory.getLogger(CatalogEventRelay.class)
                        .warn("Подія {} залишилася для повторного надсилання", event.eventId(), error);
                return;
            }
        }
    }
}
