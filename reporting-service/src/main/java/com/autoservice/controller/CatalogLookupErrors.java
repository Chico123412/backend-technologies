package com.autoservice.controller;

import com.autoservice.catalog.CatalogItemNotFound;
import com.autoservice.catalog.CatalogUnavailable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class CatalogLookupErrors {
    @ExceptionHandler(CatalogItemNotFound.class)
    public ResponseEntity<Map<String, String>> notFound(CatalogItemNotFound error) {
        return ResponseEntity.status(404).body(Map.of("message", error.getMessage()));
    }

    @ExceptionHandler(CatalogUnavailable.class)
    public ResponseEntity<Map<String, String>> unavailable(CatalogUnavailable error) {
        return ResponseEntity.status(503).body(Map.of("message", error.getMessage()));
    }
}
