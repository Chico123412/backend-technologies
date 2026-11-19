package com.autoservice.config;

import com.autoservice.grpc.CatalogGrpcApi;
import com.autoservice.grpc.CatalogGrpcServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GrpcServerConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(GrpcServerConfig.class)
            .withBean(CatalogGrpcApi.class, () -> mock(CatalogGrpcApi.class));

    @Test
    void grpcIsDisabledUnlessPropertyEnablesIt() {
        runner.run(context -> assertThat(context).doesNotHaveBean(CatalogGrpcServer.class));
    }

    @Test
    void enabledServerFollowsApplicationLifecycle() {
        CatalogGrpcServer[] server = new CatalogGrpcServer[1];
        runner.withPropertyValues("catalog.grpc.enabled=true", "catalog.grpc.port=0").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(CatalogGrpcServer.class);
            server[0] = context.getBean(CatalogGrpcServer.class);
            assertThat(server[0].isRunning()).isTrue();
            assertThat(server[0].getPort()).isPositive();
        });
        assertThat(server[0].isRunning()).isFalse();
    }
}
