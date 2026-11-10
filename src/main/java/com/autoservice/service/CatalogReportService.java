package com.autoservice.service;

import com.autoservice.dto.CatalogSummary;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class CatalogReportService {
    private final CatalogReportTasks tasks;
    private final long timeoutMillis;

    public CatalogReportService(CatalogReportTasks tasks,
                                @Value("${catalog.summary-timeout-ms:3000}") long timeoutMillis) {
        this.tasks = tasks;
        this.timeoutMillis = timeoutMillis;
    }

    public UUID startExport() {
        var id = UUID.randomUUID();
        try {
            tasks.export(id);
            return id;
        } catch (TaskRejectedException error) {
            throw busy(error);
        }
    }

    public CompletableFuture<CatalogSummary> summary() {
        try {
            var price = tasks.maxPrice();
            var duration = tasks.maxDuration();
            return price.thenCombine(duration, (p, d) -> new CatalogSummary(p, d, null))
                    .orTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                    .exceptionally(this::unavailable);
        } catch (TaskRejectedException error) {
            throw busy(error);
        }
    }

    private CatalogSummary unavailable(Throwable error) {
        Throwable cause = error instanceof CompletionException ? error.getCause() : error;
        LoggerFactory.getLogger(CatalogReportService.class).warn("Не вдалося отримати зведення каталогу", cause);
        String message = cause instanceof TimeoutException
                ? "Зведення не встигло підготуватися. Спробуйте пізніше."
                : "Зведення тимчасово недоступне. Спробуйте пізніше.";
        return new CatalogSummary(null, null, message);
    }

    private ResponseStatusException busy(TaskRejectedException error) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Черга каталогу заповнена. Спробуйте пізніше.", error);
    }
}
