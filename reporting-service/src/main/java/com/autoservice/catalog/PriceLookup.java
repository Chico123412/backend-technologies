package com.autoservice.catalog;

public interface PriceLookup {
    java.math.BigDecimal price(long id);
}
