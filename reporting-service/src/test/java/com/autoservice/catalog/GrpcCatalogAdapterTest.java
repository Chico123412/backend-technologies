package com.autoservice.catalog;

import com.autoservice.contract.CatalogRpcGrpc;
import com.autoservice.contract.ServiceData;
import com.autoservice.contract.ServiceId;
import com.autoservice.controller.CatalogLookupErrors;
import com.autoservice.controller.ServicePlanningController;
import io.grpc.*;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GrpcCatalogAdapterTest {
    private Server server;
    private ManagedChannel channel;
    private GrpcCatalogAdapter adapter;
    private volatile Status.Code failure;
    private volatile boolean silent;

    @BeforeEach
    void start() throws Exception {
        server = NettyServerBuilder.forPort(0).addService(new CatalogRpcGrpc.CatalogRpcImplBase() {
            @Override
            public void getService(ServiceId request, StreamObserver<ServiceData> observer) {
                if (silent) {
                    return;
                }
                if (failure != null) {
                    observer.onError(Status.fromCode(failure).asRuntimeException());
                    return;
                }
                observer.onNext(ServiceData.newBuilder().setId(request.getId()).setName("Огляд")
                        .setDescription("Перевірка авто").setPrice("123.45").setDurationMinutes(30).build());
                observer.onCompleted();
            }
        }).build().start();
        channel = NettyChannelBuilder.forAddress("127.0.0.1", server.getPort()).usePlaintext().build();
        adapter = new GrpcCatalogAdapter(CatalogRpcGrpc.newBlockingStub(channel), 1500);
    }

    @AfterEach
    void stop() throws Exception {
        channel.shutdownNow();
        server.shutdownNow();
        assertThat(channel.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
        assertThat(server.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void realGrpcProvidesAllThreeBusinessActions() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ServicePlanningController(adapter, adapter, adapter))
                .setControllerAdvice(new CatalogLookupErrors()).build();
        mvc.perform(get("/api/estimates/7").param("quantity", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(246.90));
        mvc.perform(get("/api/planning/7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.durationMinutes").value(30));
        mvc.perform(get("/api/cards/7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Огляд"));
    }

    @Test
    void notFoundBecomesOwnExceptionAndHttp404() throws Exception {
        failure = Status.Code.NOT_FOUND;
        assertThatThrownBy(() -> adapter.details(99)).isInstanceOf(CatalogItemNotFound.class);
        var mvc = MockMvcBuilders.standaloneSetup(new ServicePlanningController(adapter, adapter, adapter))
                .setControllerAdvice(new CatalogLookupErrors()).build();
        mvc.perform(get("/api/cards/99")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @EnumSource(value = Status.Code.class, names = {"UNAVAILABLE", "INTERNAL", "DEADLINE_EXCEEDED", "PERMISSION_DENIED"})
    void allOtherStatusesBecomeUnavailable(Status.Code code) {
        failure = code;
        assertThatThrownBy(() -> adapter.price(7)).isInstanceOf(CatalogUnavailable.class);
    }

    @Test
    void actualDeadlineBoundsWaiting() {
        silent = true;
        adapter = new GrpcCatalogAdapter(CatalogRpcGrpc.newBlockingStub(channel), 150);
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(Duration.ofSeconds(3),
                () -> assertThatThrownBy(() -> adapter.durationMinutes(7)).isInstanceOf(CatalogUnavailable.class));
    }

    @Test
    void stoppedTargetProducesHttp503() throws Exception {
        server.shutdownNow();
        assertThat(server.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
        var mvc = MockMvcBuilders.standaloneSetup(new ServicePlanningController(adapter, adapter, adapter))
                .setControllerAdvice(new CatalogLookupErrors()).build();
        mvc.perform(get("/api/cards/7")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Каталог тимчасово недоступний"));
    }
}
