package com.autoservice.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class CatalogSnapshotService {
    private final JdbcTemplate jdbc;

    public CatalogSnapshotService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int capture(String reportType, LocalDate date) {
        if (!"daily".equals(reportType) && !"weekly".equals(reportType)) {
            throw new IllegalArgumentException("Невідомий тип звіту: " + reportType);
        }
        return jdbc.update("""
                INSERT INTO catalog_snapshots (report_type, report_date, service_count)
                SELECT ?, ?, COUNT(*) FROM service_items
                ON CONFLICT DO NOTHING
                """, reportType, date);
    }

    @Transactional
    public int cleanup(LocalDate date, int retentionDays) {
        if (retentionDays <= 0) {
            throw new IllegalArgumentException("retentionDays має бути більше нуля");
        }
        return jdbc.update("DELETE FROM catalog_snapshots WHERE report_date < ?",
                date.minusDays(retentionDays));
    }
}
