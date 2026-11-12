package com.autoservice.job;

import com.autoservice.service.CatalogSnapshotService;
import org.junit.jupiter.api.Test;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogJobsTest {
    private final CatalogSnapshotService service = mock(CatalogSnapshotService.class);
    private final LocalDate date = LocalDate.of(2026, 11, 11);

    @Test
    void snapshotUsesMergedParametersAndKyivDate() throws Exception {
        new CatalogSnapshotJob(service).execute(context(Map.of("reportType", "weekly", "zoneId", "Europe/Kyiv")));
        verify(service).capture("weekly", date);
        verifyNoMoreInteractions(service);
    }

    @Test
    void cleanupPassesRetentionDaysToService() throws Exception {
        new SnapshotCleanupJob(service).execute(context(Map.of("retentionDays", "30", "zoneId", "Europe/Kyiv")));
        verify(service).cleanup(date, 30);
        verifyNoMoreInteractions(service);
    }

    @Test
    void missingParameterFailsExplicitlyWithoutCallingService() {
        assertThatThrownBy(() -> new CatalogSnapshotJob(service).execute(context(Map.of("zoneId", "Europe/Kyiv"))))
                .isInstanceOf(JobExecutionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Відсутній параметр Quartz: reportType");
        verifyNoInteractions(service);
    }

    @Test
    void failedJobDoesNotRequestImmediateRetryOrRemoveTriggers() {
        var error = new IllegalStateException("database unavailable");
        when(service.capture("daily", date)).thenThrow(error);
        assertThatThrownBy(() -> new CatalogSnapshotJob(service)
                .execute(context(Map.of("reportType", "daily", "zoneId", "Europe/Kyiv"))))
                .isInstanceOfSatisfying(JobExecutionException.class, failure -> {
                    assertThat(failure.getCause()).isSameAs(error);
                    assertThat(failure.refireImmediately()).isFalse();
                    assertThat(failure.unscheduleFiringTrigger()).isFalse();
                    assertThat(failure.unscheduleAllTriggers()).isFalse();
                });
        verify(service).capture("daily", date);
    }

    @Test
    void bothJobsDisallowConcurrentExecutions() {
        assertThat(CatalogSnapshotJob.class).hasAnnotation(DisallowConcurrentExecution.class);
        assertThat(SnapshotCleanupJob.class).hasAnnotation(DisallowConcurrentExecution.class);
    }

    private JobExecutionContext context(Map<String, Object> data) {
        var context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap(data));
        when(context.getScheduledFireTime()).thenReturn(Date.from(Instant.parse("2026-11-10T23:30:00Z")));
        return context;
    }
}
