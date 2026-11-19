package com.autoservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("catalog.lookup")
public record CatalogLookupSettings(Transport transport, String httpBaseUrl, String grpcTarget,
                                    int connectTimeoutMs, int readTimeoutMs, int deadlineMs) {
    public enum Transport { HTTP, GRPC }

    public CatalogLookupSettings {
        if (transport == null || httpBaseUrl == null || grpcTarget == null
                || connectTimeoutMs <= 0 || readTimeoutMs <= 0 || deadlineMs <= 0) {
            throw new IllegalArgumentException("Потрібні адреси каталогу й додатні тайм-аути");
        }
    }
}
