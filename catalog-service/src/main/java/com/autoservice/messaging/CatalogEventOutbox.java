package com.autoservice.messaging;

import com.autoservice.dto.ServiceResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class CatalogEventOutbox {
    private final JdbcTemplate jdbc;

    public CatalogEventOutbox(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void record(String action, ServiceResponse item) {
        jdbc.update("""
                INSERT INTO catalog_event_outbox (event_id, action, service_id, service_name)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID().toString(), action, item.id(), item.name());
    }
}
