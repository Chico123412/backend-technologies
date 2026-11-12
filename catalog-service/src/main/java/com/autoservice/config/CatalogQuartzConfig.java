package com.autoservice.config;

import com.autoservice.job.CatalogSnapshotJob;
import com.autoservice.job.SnapshotCleanupJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration(proxyBeanMethods = false)
public class CatalogQuartzConfig {
    public static final ZoneId ZONE = ZoneId.of("Europe/Kyiv");
    public static final JobKey SNAPSHOT = JobKey.jobKey("catalog-snapshot", "catalog");
    public static final JobKey CLEANUP = JobKey.jobKey("snapshot-cleanup", "catalog");

    @Bean
    JobDetail catalogSnapshotJob() {
        return JobBuilder.newJob(CatalogSnapshotJob.class)
                .withIdentity(SNAPSHOT)
                .usingJobData("reportType", "daily")
                .usingJobData("zoneId", ZONE.getId())
                .storeDurably()
                .build();
    }

    @Bean
    JobDetail snapshotCleanupJob() {
        return JobBuilder.newJob(SnapshotCleanupJob.class)
                .withIdentity(CLEANUP)
                .usingJobData("retentionDays", "30")
                .usingJobData("zoneId", ZONE.getId())
                .storeDurably()
                .build();
    }

    @Bean
    CronTrigger dailySnapshotTrigger() {
        return trigger("daily-snapshot", SNAPSHOT, "0 0 2 * * ?").build();
    }

    @Bean
    CronTrigger weeklySnapshotTrigger() {
        return trigger("weekly-snapshot", SNAPSHOT, "0 0 3 ? * MON")
                .usingJobData("reportType", "weekly")
                .build();
    }

    @Bean
    CronTrigger snapshotCleanupTrigger() {
        return trigger("daily-cleanup", CLEANUP, "0 0 4 * * ?").build();
    }

    private TriggerBuilder<CronTrigger> trigger(String name, JobKey job, String cron) {
        return TriggerBuilder.newTrigger()
                .withIdentity(name, "catalog")
                .forJob(job)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .inTimeZone(TimeZone.getTimeZone(ZONE))
                        .withMisfireHandlingInstructionDoNothing());
    }
}
