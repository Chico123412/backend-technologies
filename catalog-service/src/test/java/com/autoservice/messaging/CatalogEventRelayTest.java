package com.autoservice.messaging;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CatalogEventRelayTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final CatalogEventRelay relay = new CatalogEventRelay(jdbc, rabbit);

    @Test
    void ackMarksEventAsPublishedAndUsesPersistentMessage() {
        pending();
        doAnswer(call -> {
            var message = new Message(new byte[0]);
            call.getArgument(3, MessagePostProcessor.class).postProcessMessage(message);
            assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
            call.getArgument(4, CorrelationData.class).getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        relay.publishPending();

        verify(jdbc).update(contains("published = true"), eq("event-1"));
    }

    @Test
    void brokerFailureKeepsEventForRetry() {
        pending();
        doThrow(new IllegalStateException("broker offline")).when(rabbit).convertAndSend(anyString(),
                anyString(), any(Object.class), any(MessagePostProcessor.class), any(CorrelationData.class));
        assertThatCode(relay::publishPending).doesNotThrowAnyException();
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void nackDoesNotMarkEventAsPublished() {
        pending();
        doAnswer(call -> {
            call.getArgument(4, CorrelationData.class).getFuture().complete(new CorrelationData.Confirm(false, "nack"));
            return null;
        }).when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        relay.publishPending();
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    private void pending() {
        when(jdbc.query(anyString(), ArgumentMatchers.<RowMapper<ServiceEvent>>any()))
                .thenReturn(List.of(new ServiceEvent("event-1", "CREATED", 1L, "Огляд")));
    }
}
