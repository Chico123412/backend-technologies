package com.autoservice.grpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.context.SmartLifecycle;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CatalogGrpcServer implements SmartLifecycle {
    private final CatalogGrpcApi api;
    private final int port;
    private Server server;

    public CatalogGrpcServer(CatalogGrpcApi api, int port) {
        this.api = api;
        this.port = port;
    }

    @Override
    public void start() {
        try {
            server = NettyServerBuilder.forPort(port).addService(api).build().start();
        } catch (IOException error) {
            throw new IllegalStateException("Не вдалося запустити gRPC", error);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdownNow();
            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("gRPC не завершив роботу за 5 секунд");
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return server != null && !server.isShutdown();
    }

    public int getPort() {
        return server.getPort();
    }
}
