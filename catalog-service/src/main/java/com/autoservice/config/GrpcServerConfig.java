package com.autoservice.config;

import com.autoservice.grpc.CatalogGrpcApi;
import com.autoservice.grpc.CatalogGrpcServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcServerConfig {
    @Bean
    @ConditionalOnProperty(name = "catalog.grpc.enabled", havingValue = "true")
    CatalogGrpcServer catalogGrpcServer(CatalogGrpcApi api, @Value("${catalog.grpc.port:9090}") int port) {
        return new CatalogGrpcServer(api, port);
    }
}
