package com.autoservice.events;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CatalogEventJournalTest {
    @Test
    void duplicateDeliveryKeepsOneJournalEntry() {
        var jdbc = mock(JdbcTemplate.class);
        var ids = new HashSet<String>();
        when(jdbc.update(contains("ON CONFLICT DO NOTHING"), any(), any(), any(), any()))
                .thenAnswer(call -> ids.add(call.getArgument(1, String.class)) ? 1 : 0);
        var journal = new CatalogEventJournal(jdbc);
        var listener = new CatalogEventListener(journal);
        var event = new ServiceEvent("same-event", "CREATED", 1L, "Діагностика");

        listener.receive(event);
        listener.receive(event);

        assertThat(ids).containsExactly("same-event");
        verify(jdbc, times(2)).update(contains("ON CONFLICT DO NOTHING"), eq("same-event"),
                eq("CREATED"), eq(1L), eq("Діагностика"));
    }
}
