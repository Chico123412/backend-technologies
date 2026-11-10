package com.autoservice.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class CatalogReportTasks {
    private final CatalogReportData data;

    public CatalogReportTasks(CatalogReportData data) {
        this.data = data;
    }

    @Async("catalogExecutor")
    public void export(UUID id) {
        try {
            data.export(id);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    @Async("catalogExecutor")
    public CompletableFuture<BigDecimal> maxPrice() {
        return CompletableFuture.completedFuture(data.maxPrice());
    }

    @Async("catalogExecutor")
    public CompletableFuture<Integer> maxDuration() {
        return CompletableFuture.completedFuture(data.maxDuration());
    }
}
