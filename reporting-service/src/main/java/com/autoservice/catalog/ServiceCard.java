package com.autoservice.catalog;

import java.math.BigDecimal;

public record ServiceCard(Long id, String name, String description, BigDecimal price, int durationMinutes) {
}
