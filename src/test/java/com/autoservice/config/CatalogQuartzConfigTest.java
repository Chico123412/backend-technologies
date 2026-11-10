package com.autoservice.config;

import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogQuartzConfigTest {
    private final CatalogQuartzConfig config = new CatalogQuartzConfig();

    @Test
    void triggersLinkToTheirJobsAndSetTimezoneAndMisfirePolicy() {
        var daily = config.dailySnapshotTrigger();
        var weekly = config.weeklySnapshotTrigger();
        var cleanup = config.snapshotCleanupTrigger();

        assertThat(daily.getJobKey()).isEqualTo(config.catalogSnapshotJob().getKey());
        assertThat(weekly.getJobKey()).isEqualTo(daily.getJobKey());
        assertThat(cleanup.getJobKey()).isEqualTo(config.snapshotCleanupJob().getKey());
        assertThat(List.of(daily.getKey().getName(), weekly.getKey().getName(), cleanup.getKey().getName()))
                .containsExactly("daily-snapshot", "weekly-snapshot", "daily-cleanup");
        for (var trigger : List.of(daily, weekly, cleanup)) {
            assertThat(trigger.getKey().getGroup()).isEqualTo("catalog");
            assertThat(trigger.getTimeZone().getID()).isEqualTo("Europe/Kyiv");
            assertThat(trigger.getMisfireInstruction()).isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        }
    }

    @Test
    void weeklyOverridesTypeAndKeepsDefaultTimezone() {
        var defaults = config.catalogSnapshotJob().getJobDataMap();
        assertThat(defaults.getString("reportType")).isEqualTo("daily");
        assertThat(config.dailySnapshotTrigger().getJobDataMap()).doesNotContainKey("reportType");
        var merged = new JobDataMap(defaults);
        merged.putAll(config.weeklySnapshotTrigger().getJobDataMap());

        assertThat(merged.getString("reportType")).isEqualTo("weekly");
        assertThat(merged.getString("zoneId")).isEqualTo("Europe/Kyiv");
        assertThat(config.snapshotCleanupJob().getJobDataMap().getString("retentionDays")).isEqualTo("30");
    }
}
