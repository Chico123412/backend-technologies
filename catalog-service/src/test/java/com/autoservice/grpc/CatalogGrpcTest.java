package com.autoservice.grpc;

import com.autoservice.contract.CatalogRpcGrpc;
import com.autoservice.contract.ServiceId;
import com.autoservice.dto.ServiceResponse;
import com.autoservice.service.CatalogService;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogGrpcTest {
    private final CatalogService catalog = mock(CatalogService.class);
    private final CatalogGrpcServer server = new CatalogGrpcServer(new CatalogGrpcApi(catalog), 0);
    private ManagedChannel channel;

    @BeforeEach
    void start() {
        server.start();
        channel = NettyChannelBuilder.forAddress("127.0.0.1", server.getPort()).usePlaintext().build();
    }

    @AfterEach
    void stop() throws Exception {
        channel.shutdownNow();
        assertThat(channel.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
        server.stop();
        assertThat(server.isRunning()).isFalse();
    }

    @Test
    void actualServerReturnsCatalogData() {
        when(catalog.findById(7L)).thenReturn(new ServiceResponse(7L, "Огляд", null, new BigDecimal("123.45"), 30));
        var item = stub().getService(ServiceId.newBuilder().setId(7).build());

        assertThat(server.isRunning()).isTrue();
        assertThat(item.getName()).isEqualTo("Огляд");
        assertThat(item.getDescription()).isEmpty();
        assertThat(item.getPrice()).isEqualTo("123.45");
        assertThat(item.getDurationMinutes()).isEqualTo(30);
    }

    @Test
    void missingServiceReturnsNotFound() {
        when(catalog.findById(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> stub().getService(ServiceId.newBuilder().setId(99).build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        error -> assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
    }

    @Test
    void databaseErrorReturnsInternal() {
        when(catalog.findById(7L)).thenThrow(new IllegalStateException("database unavailable"));
        assertThatThrownBy(() -> stub().getService(ServiceId.newBuilder().setId(7).build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        error -> assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL));
    }

    private CatalogRpcGrpc.CatalogRpcBlockingStub stub() {
        return CatalogRpcGrpc.newBlockingStub(channel).withDeadlineAfter(2, TimeUnit.SECONDS);
    }
}
