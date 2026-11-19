package com.autoservice.events;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CatalogEventJournal {
    private final JdbcTemplate jdbc;

    public CatalogEventJournal(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int accept(ServiceEvent event) {
        return jdbc.update("""
                INSERT INTO catalog_event_journal (event_id, action, service_id, service_name)
                VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING
                """, event.eventId(), event.action(), event.serviceId(), event.name());
    }

    public List<ServiceEvent> recent() {
        return jdbc.query("""
                SELECT event_id, action, service_id, service_name FROM catalog_event_journal
                ORDER BY received_at DESC, event_id LIMIT 100
                """, (row, index) -> new ServiceEvent(row.getString(1), row.getString(2),
                row.getLong(3), row.getString(4)));
    }
}
