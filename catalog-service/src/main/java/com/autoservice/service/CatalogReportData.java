package com.autoservice.service;

import com.autoservice.repository.ServiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class CatalogReportData {
    private final CatalogService catalog;
    private final ServiceRepository repository;
    private final ObjectMapper json;
    private final Path exportDirectory;
    private final long delayMillis;

    public CatalogReportData(CatalogService catalog, ServiceRepository repository, ObjectMapper json,
                             @Value("${catalog.export-directory:target/catalog-exports}") String exportDirectory,
                             @Value("${catalog.async-delay-ms:750}") long delayMillis) {
        this.catalog = catalog;
        this.repository = repository;
        this.json = json;
        this.exportDirectory = Path.of(exportDirectory);
        this.delayMillis = delayMillis;
    }

    public void export(UUID id) throws IOException {
        pause();
        Files.createDirectories(exportDirectory);
        json.writerWithDefaultPrettyPrinter()
                .writeValue(exportDirectory.resolve(id + ".json").toFile(), catalog.findAll());
    }

    public BigDecimal maxPrice() {
        pause();
        return repository.findMaxPrice();
    }

    public Integer maxDuration() {
        pause();
        return repository.findMaxDuration();
    }

    private void pause() {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Операцію перервано", error);
        }
    }
}
