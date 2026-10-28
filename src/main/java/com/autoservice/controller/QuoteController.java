package com.autoservice.controller;

import com.autoservice.annotation.ApiController;
import com.autoservice.service.QuoteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@ApiController("/api/quotes")
public class QuoteController {
    private final QuoteService quotes;

    public QuoteController(QuoteService quotes) {
        this.quotes = quotes;
    }

    @PostMapping
    public Map<String, BigDecimal> calculate(@RequestBody List<Long> serviceIds) {
        return Map.of("total", quotes.calculateTotal(serviceIds));
    }

    @PostMapping("/self-invocation")
    public Map<String, BigDecimal> selfInvocation(@RequestBody List<Long> serviceIds) {
        return Map.of("total", quotes.calculateTotalInternally(serviceIds));
    }
}
