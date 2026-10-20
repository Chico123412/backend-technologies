package com.autoservice.controller;

import com.autoservice.dto.ServiceRequest;
import com.autoservice.dto.ServiceResponse;
import com.autoservice.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
    private final CatalogService service;

    public ServiceController(CatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<ServiceResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ServiceResponse findById(@PathVariable long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody ServiceRequest request) {
        ServiceResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/services/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ServiceResponse update(@PathVariable long id, @Valid @RequestBody ServiceRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
