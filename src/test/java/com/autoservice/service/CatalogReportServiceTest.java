package com.autoservice.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogReportServiceTest {
    @Test
    void summaryWaitHasATimeoutEvenIfOneQueryNeverFinishes() throws Exception {
        var tasks = mock(CatalogReportTasks.class);
        var price = new CompletableFuture<BigDecimal>();
        when(tasks.maxPrice()).thenReturn(price);
        when(tasks.maxDuration()).thenReturn(CompletableFuture.completedFuture(45));
        var report = new CatalogReportService(tasks, 30);

        var result = report.summary().get(2, TimeUnit.SECONDS);

        assertThat(result.message()).isEqualTo("Зведення не встигло підготуватися. Спробуйте пізніше.");
        assertThat(result.maxPrice()).isNull();
        assertThat(price).isNotDone();
    }
}
