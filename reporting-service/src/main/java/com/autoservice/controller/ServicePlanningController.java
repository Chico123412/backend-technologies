package com.autoservice.controller;

import com.autoservice.catalog.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Map;

@RestController
public class ServicePlanningController {
    private final PriceLookup prices;
    private final DurationLookup durations;
    private final ServiceDetailsLookup details;

    public ServicePlanningController(PriceLookup prices, DurationLookup durations, ServiceDetailsLookup details) {
        this.prices = prices;
        this.durations = durations;
        this.details = details;
    }

    @GetMapping("/api/estimates/{id}")
    public Map<String, BigDecimal> estimate(@PathVariable long id, @RequestParam(defaultValue = "1") int quantity) {
        if (quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Кількість має бути додатною");
        }
        return Map.of("total", prices.price(id).multiply(BigDecimal.valueOf(quantity)));
    }

    @GetMapping("/api/planning/{id}")
    public Map<String, Integer> planning(@PathVariable long id) {
        return Map.of("durationMinutes", durations.durationMinutes(id));
    }

    @GetMapping("/api/cards/{id}")
    public ServiceCard card(@PathVariable long id) {
        return details.details(id);
    }
}
