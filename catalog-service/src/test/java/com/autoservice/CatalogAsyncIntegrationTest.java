package com.autoservice;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.autoservice.config.AsyncConfig;
import com.autoservice.controller.CatalogReportController;
import com.autoservice.service.CatalogReportData;
import com.autoservice.service.CatalogReportService;
import com.autoservice.service.CatalogReportTasks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(CatalogAsyncIntegrationTest.Config.class)
class CatalogAsyncIntegrationTest {
    @TestConfiguration
    @Import({AsyncConfig.class, CatalogReportTasks.class, CatalogReportService.class, CatalogReportController.class})
    static class Config {}

    @MockitoBean CatalogReportData data;
    @Autowired CatalogReportController controller;
    @Autowired @Qualifier("catalogExecutor") ThreadPoolTaskExecutor executor;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void exportReturns202BeforeWorkerFinishes() throws Exception {
        var started = new CompletableFuture<String>();
        var finished = new CompletableFuture<Void>();
        var release = new CountDownLatch(1);
        doAnswer(call -> {
            started.complete(Thread.currentThread().getName());
            try {
                await(release);
            } finally {
                finished.complete(null);
            }
            return null;
        }).when(data).export(any());

        try {
            mvc.perform(post("/api/admin/catalog/export"))
                    .andExpect(status().isAccepted()).andExpect(jsonPath("$.exportId").isString());
            assertThat(started.get(2, TimeUnit.SECONDS)).startsWith("catalog-async-");
            assertThat(finished).isNotDone();
        } finally {
            release.countDown();
            finished.get(3, TimeUnit.SECONDS);
        }
    }

    @Test
    void summaryStartsBothQueriesBeforeEitherCanFinish() throws Exception {
        var priceThread = new CompletableFuture<String>();
        var durationThread = new CompletableFuture<String>();
        var release = new CountDownLatch(1);
        when(data.maxPrice()).thenAnswer(call -> {
            priceThread.complete(Thread.currentThread().getName());
            await(release);
            return new BigDecimal("450.00");
        });
        when(data.maxDuration()).thenAnswer(call -> {
            durationThread.complete(Thread.currentThread().getName());
            await(release);
            return 80;
        });

        var result = mvc.perform(get("/api/admin/catalog/summary"))
                .andExpect(request().asyncStarted()).andReturn();
        try {
            assertThat(priceThread.get(2, TimeUnit.SECONDS)).startsWith("catalog-async-");
            assertThat(durationThread.get(2, TimeUnit.SECONDS)).startsWith("catalog-async-")
                    .isNotEqualTo(priceThread.getNow(null));
        } finally {
            release.countDown();
        }
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                .andExpect(jsonPath("$.maxPrice").value(450))
                .andExpect(jsonPath("$.maxDurationMinutes").value(80));
    }

    @Test
    void failedExportIsLoggedByTheVoidExceptionHandler() throws Exception {
        doThrow(new IOException("disk unavailable")).when(data).export(any());
        var logged = new CompletableFuture<ILoggingEvent>();
        var logger = (Logger) LoggerFactory.getLogger(AsyncConfig.class);
        var appender = new AppenderBase<ILoggingEvent>() {
            @Override protected void append(ILoggingEvent event) {
                logged.complete(event);
            }
        };
        appender.start();
        logger.addAppender(appender);
        try {
            mvc.perform(post("/api/admin/catalog/export")).andExpect(status().isAccepted());
            var event = logged.get(3, TimeUnit.SECONDS);
            assertThat(event.getFormattedMessage()).contains("export", "помилкою");
            assertThat(event.getThrowableProxy().getCause().getMessage()).isEqualTo("disk unavailable");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void failedQueryReturnsFallbackInsteadOf500() throws Exception {
        when(data.maxPrice()).thenThrow(new IllegalStateException("query failed"));
        when(data.maxDuration()).thenReturn(80);
        var result = mvc.perform(get("/api/admin/catalog/summary"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Зведення тимчасово недоступне. Спробуйте пізніше."));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void fullExecutorReturns503AndAnExplicitRetryMessage() throws Exception {
        var running = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        var done = new CountDownLatch(4);
        Runnable occupied = () -> {
            running.countDown();
            try {
                await(release);
            } finally {
                done.countDown();
            }
        };
        try {
            executor.execute(occupied);
            executor.execute(occupied);
            await(running);
            executor.execute(occupied);
            executor.execute(occupied);
            assertThat(executor.getThreadPoolExecutor().getQueue()).hasSize(2);
            mvc.perform(post("/api/admin/catalog/export")).andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("Черга каталогу заповнена. Спробуйте пізніше."));
            mvc.perform(get("/api/admin/catalog/summary")).andExpect(status().isServiceUnavailable());
        } finally {
            release.countDown();
        }
        await(done);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Очікування в тесті перевищило 5 секунд");
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }
}
