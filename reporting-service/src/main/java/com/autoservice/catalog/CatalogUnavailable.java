package com.autoservice.catalog;

public class CatalogUnavailable extends RuntimeException {
    public CatalogUnavailable(Throwable cause) { super("Каталог тимчасово недоступний", cause); }
}
