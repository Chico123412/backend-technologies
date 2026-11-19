package com.autoservice.catalog;

import java.math.BigDecimal;

public abstract class CatalogLookupAdapter implements PriceLookup, DurationLookup, ServiceDetailsLookup {
    protected abstract ServiceCard find(long id);

    @Override
    public BigDecimal price(long id) {
        return find(id).price();
    }

    @Override
    public int durationMinutes(long id) {
        return find(id).durationMinutes();
    }

    @Override
    public ServiceCard details(long id) {
        return find(id);
    }
}
