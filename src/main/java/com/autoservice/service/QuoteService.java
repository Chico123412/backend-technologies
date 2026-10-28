package com.autoservice.service;

import com.autoservice.annotation.LimitBatchSize;
import com.autoservice.annotation.RoundMoney;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class QuoteService {
    private final CatalogService catalog;

    public QuoteService(CatalogService catalog) {
        this.catalog = catalog;
    }

    @LimitBatchSize(max = 10)
    @RoundMoney
    public BigDecimal calculateTotal(List<Long> serviceIds) {
        BigDecimal total = BigDecimal.ZERO;
        for (Long id : serviceIds) {
            total = total.add(catalog.findById(id).price());
        }
        return serviceIds.size() >= 2 ? total.multiply(new BigDecimal("0.95")) : total;
    }

    // Навмисний self-invocation для порівняння з викликом через Spring-проксі.
    public BigDecimal calculateTotalInternally(List<Long> serviceIds) {
        return this.calculateTotal(serviceIds);
    }
}
