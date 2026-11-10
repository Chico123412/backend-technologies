package com.autoservice.job;

import org.quartz.JobExecutionContext;

import java.time.LocalDate;
import java.time.ZoneId;

final class JobParameters {
    private JobParameters() {}

    static String required(JobExecutionContext context, String key) {
        String value = context.getMergedJobDataMap().getString(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Відсутній параметр Quartz: " + key);
        }
        return value;
    }

    static LocalDate scheduledDate(JobExecutionContext context) {
        ZoneId zone = ZoneId.of(required(context, "zoneId"));
        return context.getScheduledFireTime().toInstant().atZone(zone).toLocalDate();
    }
}
