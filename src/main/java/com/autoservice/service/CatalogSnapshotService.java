package com.autoservice.service;

import com.autoservice.repository.ServiceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class CatalogSnapshotService {
    private final JdbcTemplate jdbc;
    private final ServiceRepository catalog;

    public CatalogSnapshotService(JdbcTemplate jdbc, ServiceRepository catalog) {
        this.jdbc = jdbc;
        this.catalog = catalog;
    }

    @Transactional
    public int capture(String reportType, LocalDate date) {
        if (!"daily".equals(reportType) && !"weekly".equals(reportType)) {
            throw new IllegalArgumentException("Невідомий тип звіту: " + reportType);
        }
        return jdbc.update("""
                INSERT INTO catalog_snapshots (report_type, report_date, service_count)
                VALUES (?, ?, ?)
                ON CONFLICT DO NOTHING
                """, reportType, date, catalog.count());
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
