package com.autoservice.dto;

import java.math.BigDecimal;

public record CatalogSummary(BigDecimal maxPrice, Integer maxDurationMinutes, String message) {
}
