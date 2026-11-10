package com.autoservice.controller;

import com.autoservice.annotation.ApiController;
import com.autoservice.dto.CatalogSummary;
import com.autoservice.service.CatalogReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@ApiController("/api/admin/catalog")
public class CatalogReportController {
    private final CatalogReportService reports;

    public CatalogReportController(CatalogReportService reports) {
        this.reports = reports;
    }

    @PostMapping("/export")
    public ResponseEntity<Map<String, String>> export() {
        return ResponseEntity.accepted().body(Map.of("exportId", reports.startExport().toString()));
    }

    @GetMapping("/summary")
    public CompletableFuture<CatalogSummary> summary() {
        return reports.summary();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> reportError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason()));
    }
}
