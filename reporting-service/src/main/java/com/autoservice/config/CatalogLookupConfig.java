package com.autoservice.config;

import com.autoservice.catalog.*;
import com.autoservice.contract.CatalogRpcGrpc;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(CatalogLookupSettings.class)
public class CatalogLookupConfig {
    @Bean(destroyMethod = "shutdownNow")
    @ConditionalOnProperty(name = "catalog.lookup.transport", havingValue = "grpc")
    ManagedChannel catalogChannel(CatalogLookupSettings settings) {
        return NettyChannelBuilder.forTarget(settings.grpcTarget()).usePlaintext().build();
    }

    @Bean
    CatalogLookupAdapter catalogLookup(CatalogLookupSettings settings, ObjectProvider<ManagedChannel> channel) {
        if (settings.transport() == CatalogLookupSettings.Transport.GRPC) {
            return new GrpcCatalogAdapter(CatalogRpcGrpc.newBlockingStub(channel.getObject()), settings.deadlineMs());
        }
        return new HttpCatalogAdapter(httpApi(settings));
    }

    public static CatalogHttpApi httpApi(CatalogLookupSettings settings) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(settings.connectTimeoutMs());
        requestFactory.setReadTimeout(settings.readTimeoutMs());
        var client = RestClient.builder().baseUrl(settings.httpBaseUrl()).requestFactory(requestFactory).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client)).build()
                .createClient(CatalogHttpApi.class);
    }
}
