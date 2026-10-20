package com.autoservice.controller;

import com.autoservice.repository.ServiceRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class AdminController {
    private final ServiceRepository repository;

    public AdminController(ServiceRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/admin/summary")
    public Map<String, Long> summary() {
        return Map.of("serviceCount", repository.count());
    }
}
