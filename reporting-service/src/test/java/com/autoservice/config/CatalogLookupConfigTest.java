package com.autoservice.config;

import com.autoservice.catalog.*;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogLookupConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CatalogLookupConfig.class)
            .withPropertyValues("catalog.lookup.http-base-url=http://localhost:8080",
                    "catalog.lookup.grpc-target=localhost:9090",
                    "catalog.lookup.connect-timeout-ms=200",
                    "catalog.lookup.read-timeout-ms=300", "catalog.lookup.deadline-ms=400");

    @Test
    void httpModeCreatesOneAdapterForAllThreePorts() {
        runner.withPropertyValues("catalog.lookup.transport=http").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PriceLookup.class)
                    .hasSingleBean(DurationLookup.class).hasSingleBean(ServiceDetailsLookup.class);
            assertThat(context.getBean(PriceLookup.class)).isInstanceOf(HttpCatalogAdapter.class);
            assertThat(context).doesNotHaveBean(ManagedChannel.class);
        });
    }

    @Test
    void grpcModeCreatesGrpcAdapterAndClosesChannel() {
        ManagedChannel[] channel = new ManagedChannel[1];
        runner.withPropertyValues("catalog.lookup.transport=grpc").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(PriceLookup.class)).isInstanceOf(GrpcCatalogAdapter.class);
            channel[0] = context.getBean(ManagedChannel.class);
            assertThat(channel[0].isShutdown()).isFalse();
        });
        assertThat(channel[0].isShutdown()).isTrue();
    }

    @Test
    void unknownTransportPreventsStartup() {
        runner.withPropertyValues("catalog.lookup.transport=wrong").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("wrong");
        });
    }

    @Test
    void zeroTimeoutPreventsStartup() {
        runner.withPropertyValues("catalog.lookup.transport=http", "catalog.lookup.read-timeout-ms=0")
                .run(context -> assertThat(context).hasFailed());
    }
}
