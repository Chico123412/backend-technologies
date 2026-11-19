package com.autoservice.catalog;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface CatalogHttpApi {
    @GetExchange("/internal/catalog/{id}")
    ServiceCard find(@PathVariable long id);
}
