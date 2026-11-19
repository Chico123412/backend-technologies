package com.autoservice.catalog;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class HttpCatalogAdapter extends CatalogLookupAdapter {
    private final CatalogHttpApi api;

    public HttpCatalogAdapter(CatalogHttpApi api) {
        this.api = api;
    }

    @Override
    protected ServiceCard find(long id) {
        try {
            ServiceCard item = api.find(id);
            if (item == null) {
                throw new CatalogUnavailable(new IllegalStateException("Порожня відповідь каталогу"));
            }
            return item;
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 404) {
                throw new CatalogItemNotFound(id);
            }
            throw new CatalogUnavailable(error);
        } catch (RestClientException error) {
            throw new CatalogUnavailable(error);
        }
    }
}
