package com.autoservice.controller;

import com.autoservice.annotation.ApiController;
import com.autoservice.dto.ServiceResponse;
import com.autoservice.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@ApiController("/internal/catalog")
public class InternalCatalogController {
    private final CatalogService catalog;

    public InternalCatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/{id}")
    public ServiceResponse find(@PathVariable long id) {
        return catalog.findById(id);
    }
}
