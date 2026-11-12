package com.autoservice.controller;

import com.autoservice.annotation.ApiController;
import com.autoservice.repository.ServiceRepository;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Map;

@ApiController("/api/admin")
public class AdminController {
    private final ServiceRepository repository;

    public AdminController(ServiceRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/summary")
    public Map<String, Long> summary() {
        return Map.of("serviceCount", repository.count());
    }
}
