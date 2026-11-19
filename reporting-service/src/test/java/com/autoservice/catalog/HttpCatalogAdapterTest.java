package com.autoservice.catalog;

import com.autoservice.config.CatalogLookupConfig;
import com.autoservice.config.CatalogLookupSettings;
import com.autoservice.controller.CatalogLookupErrors;
import com.autoservice.controller.ServicePlanningController;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HttpCatalogAdapterTest {
    private HttpServer server;
    private HttpCatalogAdapter adapter;
    private volatile int status = 200;
    private volatile long delay;
    private volatile String body = """
            {"id":7,"name":"Огляд","description":"Перевірка авто","price":123.45,"durationMinutes":30}
            """;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/catalog", exchange -> {
            try {
                if (delay > 0) {
                    Thread.sleep(delay);
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        var settings = new CatalogLookupSettings(CatalogLookupSettings.Transport.HTTP,
                "http://127.0.0.1:" + server.getAddress().getPort(), "unused:9090", 200, 200, 200);
        adapter = new HttpCatalogAdapter(CatalogLookupConfig.httpApi(settings));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void declarativeClientSuppliesAllThreeBusinessActions() throws Exception {
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
    void missingResourceBecomesOwnExceptionAndHttp404() throws Exception {
        status = 404;
        assertThatThrownBy(() -> adapter.price(99)).isInstanceOf(CatalogItemNotFound.class);
        var mvc = MockMvcBuilders.standaloneSetup(new ServicePlanningController(adapter, adapter, adapter))
                .setControllerAdvice(new CatalogLookupErrors()).build();
        mvc.perform(get("/api/cards/99")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 500, 502, 503})
    void otherErrorsBecomeUnavailable(int code) {
        status = code;
        assertThatThrownBy(() -> adapter.price(7)).isInstanceOf(CatalogUnavailable.class);
    }

    @Test
    void slowResponseHitsConfiguredReadTimeout() {
        delay = 1000;
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(Duration.ofSeconds(3),
                () -> assertThatThrownBy(() -> adapter.price(7)).isInstanceOf(CatalogUnavailable.class));
    }

    @Test
    void stoppedTargetBecomesUnavailableAndHttp503() throws Exception {
        server.stop(0);
        assertThatThrownBy(() -> adapter.price(7)).isInstanceOf(CatalogUnavailable.class);
        var mvc = MockMvcBuilders.standaloneSetup(new ServicePlanningController(adapter, adapter, adapter))
                .setControllerAdvice(new CatalogLookupErrors()).build();
        mvc.perform(get("/api/planning/7")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Каталог тимчасово недоступний"));
    }

    @Test
    void malformedJsonDoesNotLeakRestClientException() {
        body = "not json";
        assertThatThrownBy(() -> adapter.details(7)).isInstanceOf(CatalogUnavailable.class);
    }
}
