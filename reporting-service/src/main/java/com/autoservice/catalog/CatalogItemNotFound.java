package com.autoservice.catalog;

public class CatalogItemNotFound extends RuntimeException {
    public CatalogItemNotFound(long id) { super("Послугу " + id + " не знайдено"); }
}
