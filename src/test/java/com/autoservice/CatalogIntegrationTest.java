package com.autoservice;

import com.autoservice.config.CatalogQuartzConfig;
import com.autoservice.service.CatalogSnapshotService;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.TriggerBuilder;
import org.quartz.listeners.JobListenerSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.autoservice.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ServiceRepository repository;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean ClientRegistrationRepository clients;

    private static final String BODY = """
            {"name":"Заміна масла","description":"Моторна олива", "price":500.00,"durationMinutes":45}
            """;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        when(decoder.decode("admin-token")).thenReturn(token("admin-token", "ADMIN"));
        when(decoder.decode("user-token")).thenReturn(token("user-token", "USER"));
        when(decoder.decode("invalid-token")).thenThrow(new BadJwtException("Invalid token"));
        when(clients.findByRegistrationId("keycloak")).thenReturn(ClientRegistration.withRegistrationId("keycloak")
                .clientId("autoservice-web").clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile")
                .authorizationUri("http://localhost:8081/realms/autoservice/protocol/openid-connect/auth")
                .tokenUri("http://localhost:8081/realms/autoservice/protocol/openid-connect/token")
                .jwkSetUri("http://localhost:8081/realms/autoservice/protocol/openid-connect/certs")
                .userInfoUri("http://localhost:8081/realms/autoservice/protocol/openid-connect/userinfo")
                .userNameAttributeName("sub").clientName("Keycloak").build());
    }

    @Autowired CatalogSnapshotService snapshots;
    @Autowired JdbcTemplate jdbc;
    @Autowired Scheduler scheduler;
    @Autowired javax.sql.DataSource dataSource;

    @Test
    void snapshotAndCleanupAreIdempotentInDatabase() {
        var day = java.time.LocalDate.of(2026, 11, 10);
        jdbc.update("DELETE FROM catalog_snapshots");
        assertThat(snapshots.capture("daily", day.minusDays(31))).isEqualTo(1);
        assertThat(snapshots.capture("daily", day.minusDays(30))).isEqualTo(1);
        assertThat(snapshots.capture("daily", day)).isEqualTo(1);
        assertThat(snapshots.capture("daily", day)).isZero();
        assertThat(snapshots.cleanup(day, 30)).isEqualTo(1);
        assertThat(snapshots.cleanup(day, 30)).isZero();
        assertThat(jdbc.queryForList("SELECT report_date FROM catalog_snapshots ORDER BY report_date",
                java.sql.Date.class)).containsExactly(java.sql.Date.valueOf(day.minusDays(30)), java.sql.Date.valueOf(day));
    }

    @Test
    void quartzExecutesRealJobUsingJdbcStoreAndSpringService() throws Exception {
        assertThat(scheduler.getMetaData().isJobStoreSupportsPersistence()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM QRTZ_JOB_DETAILS WHERE SCHED_NAME = ?",
                Integer.class, scheduler.getSchedulerName())).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM QRTZ_TRIGGERS WHERE SCHED_NAME = ?",
                Integer.class, scheduler.getSchedulerName())).isEqualTo(3);
        jdbc.update("DELETE FROM catalog_snapshots WHERE report_type = 'weekly'");
        var finished = new java.util.concurrent.CompletableFuture<JobExecutionException>();
        var listener = new JobListenerSupport() {
            @Override public String getName() { return "snapshot-test"; }

            @Override
            public void jobWasExecuted(JobExecutionContext context, JobExecutionException error) {
                if ("weekly".equals(context.getMergedJobDataMap().getString("reportType"))) {
                    finished.complete(error);
                }
            }
        };
        scheduler.getListenerManager().addJobListener(listener);
        try {
            scheduler.start();
            scheduler.triggerJob(CatalogQuartzConfig.SNAPSHOT, new JobDataMap(Map.of("reportType", "weekly")));
            assertThat(finished.get(5, java.util.concurrent.TimeUnit.SECONDS)).isNull();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM catalog_snapshots WHERE report_type = 'weekly'",
                    Integer.class)).isEqualTo(1);
        } finally {
            scheduler.standby();
            scheduler.getListenerManager().removeJobListener(listener.getName());
        }
    }

    @Test
    void triggerSurvivesSchedulerRestartUsingTheSameDatabase() throws Exception {
        var job = scheduler.getJobDetail(CatalogQuartzConfig.SNAPSHOT);
        var key = org.quartz.TriggerKey.triggerKey("restart-check", "catalog");
        var start = java.util.Date.from(Instant.parse("2099-01-01T00:00:00Z"));
        var first = restartTestScheduler();
        try {
            first.getScheduler().addJob(job, false);
            first.getScheduler().scheduleJob(TriggerBuilder.newTrigger()
                    .withIdentity(key).forJob(job.getKey()).startAt(start).build());
        } finally {
            first.destroy();
        }
        var second = restartTestScheduler();
        try {
            var restored = second.getScheduler().getTrigger(key);
            assertThat(restored).isNotNull();
            assertThat(restored.getJobKey()).isEqualTo(job.getKey());
            assertThat(restored.getNextFireTime()).isEqualTo(start);
        } finally {
            second.destroy();
        }
    }

    private SchedulerFactoryBean restartTestScheduler() throws Exception {
        var factory = new SchedulerFactoryBean();
        factory.setSchedulerName("restart-check-scheduler");
        factory.setDataSource(dataSource);
        factory.setAutoStartup(false);
        factory.afterPropertiesSet();
        return factory;
    }

    @Test
    void adminSummaryCombinesActualCatalogQueries() throws Exception {
        mvc.perform(post("/api/services").header("Authorization", "Bearer admin-token")
                .contentType("application/json").content(BODY)).andExpect(status().isCreated());
        mvc.perform(post("/api/services").header("Authorization", "Bearer admin-token")
                .contentType("application/json").content(BODY.replace("500.00", "300.00").replace("45", "90")))
                .andExpect(status().isCreated());

        var result = mvc.perform(get("/api/admin/catalog/summary")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                .andExpect(jsonPath("$.maxPrice").value(500))
                .andExpect(jsonPath("$.maxDurationMinutes").value(90));
        mvc.perform(get("/api/admin/catalog/summary").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crudPersistsChangesAndReturnsExpectedStatuses() throws Exception {
        String body = mvc.perform(post("/api/services").header("Authorization", "Bearer admin-token")
                        .contentType("application/json").content(BODY))
                .andExpect(status().isCreated()).andExpect(header().string("Location", containsString("/api/services/")))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(body).get("id").asLong();
        assertThat(repository.findById(id).orElseThrow().getPrice()).isEqualByComparingTo(new BigDecimal("500"));
        mvc.perform(get("/api/services/{id}", id).header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.durationMinutes").value(45));
        mvc.perform(put("/api/services/{id}", id).header("Authorization", "Bearer admin-token")
                        .contentType("application/json").content(BODY.replace("500.00", "650.00")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(650));
        assertThat(repository.findById(id).orElseThrow().getPrice()).isEqualByComparingTo(new BigDecimal("650"));
        mvc.perform(delete("/api/services/{id}", id).header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());
        assertThat(repository.findById(id)).isEmpty();
        mvc.perform(get("/api/services/{id}", id).header("Authorization", "Bearer user-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousApiRequestReturns401() throws Exception {
        mvc.perform(get("/api/services")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mvc.perform(get("/api/services").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uiSessionCannotAuthenticateApi() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("manager", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        mvc.perform(get("/api/services").sessionAttr("SPRING_SECURITY_CONTEXT", new SecurityContextImpl(authentication)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void filterChainBlocksUserFromAdminSummary() throws Exception {
        mvc.perform(get("/api/admin/summary").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/summary").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.serviceCount").value(0));
    }

    @Test
    void preAuthorizeBlocksUserFromWriting() throws Exception {
        mvc.perform(post("/api/services").header("Authorization", "Bearer user-token")
                        .contentType("application/json").content(BODY)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/services/999").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
        assertThat(repository.count()).isZero();
    }

    @Test
    void invalidInputReturns400WithoutSaving() throws Exception {
        mvc.perform(post("/api/services").header("Authorization", "Bearer admin-token")
                        .contentType("application/json").content(BODY.replace("500.00", "-10")))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test
    void anonymousUiRequestRedirectsToLogin() throws Exception {
        mvc.perform(get("/services")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/keycloak"));
    }

    @Test
    void loginStartsRedirectToKeycloak() throws Exception {
        mvc.perform(get("/oauth2/authorization/keycloak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("localhost:8081/realms/autoservice/protocol/openid-connect/auth")));
    }

    @Test
    void uiShowsCatalogForOidcUser() throws Exception {
        mvc.perform(get("/services").with(oidcLogin()))
                .andExpect(status().isOk()).andExpect(view().name("catalog"));
    }

    @Test
    void adminFormContainsCsrfAndRejectsMissingToken() throws Exception {
        mvc.perform(get("/services/new").with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk()).andExpect(content().string(containsString("_csrf")));
        mvc.perform(post("/services").with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .param("name", "Test").param("price", "50").param("durationMinutes", "30"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateThroughUi() throws Exception {
        mvc.perform(post("/services").with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))).with(csrf())
                        .param("name", "Діагностика").param("description", "Огляд авто").param("price", "300").param("durationMinutes", "30"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/services"));
        assertThat(repository.findAll()).singleElement().extracting("name").isEqualTo("Діагностика");
    }

    @Test
    void profileUsesUsernameResolvedFromBearerToken() throws Exception {
        var jwt = Jwt.withTokenValue("profile-token").header("alg", "RS256").subject("internal-id")
                .claim("preferred_username", "client")
                .claim("realm_access", Map.of("roles", List.of("USER"))).build();
        when(decoder.decode("profile-token")).thenReturn(jwt);

        mvc.perform(get("/api/profile").header("Authorization", "Bearer profile-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("client"));
    }

    @Test
    void profileWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
    }

    @Test
    void longServiceWithoutDescriptionIsRejectedThroughApi() throws Exception {
        String body = """
                {"name":"Діагностика","description":" ","price":300,"durationMinutes":90}
                """;
        mvc.perform(post("/api/services").header("Authorization", "Bearer admin-token")
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test
    void longServiceWithDescriptionIsAcceptedThroughApi() throws Exception {
        mvc.perform(post("/api/services").header("Authorization", "Bearer admin-token")
                        .contentType("application/json").content(BODY.replace("45", "90")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.durationMinutes").value(90));
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void longServiceWithoutDescriptionShowsFieldErrorInUi() throws Exception {
        mvc.perform(post("/services").with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .with(csrf()).param("name", "Діагностика").param("description", " ")
                        .param("price", "300").param("durationMinutes", "90"))
                .andExpect(status().isOk()).andExpect(view().name("service-form"))
                .andExpect(model().attributeHasFieldErrors("form", "description"))
                .andExpect(content().string(containsString("Для послуги тривалістю понад 60 хвилин потрібно вказати опис")));
        assertThat(repository.count()).isZero();
    }

    @Test
    void quoteEndpointsDemonstrateExternalAndInternalCalls() throws Exception {
        var item = new com.autoservice.entity.ServiceItem();
        item.setName("Діагностика");
        item.setDescription("Огляд авто");
        item.setPrice(new BigDecimal("100.01"));
        item.setDurationMinutes(30);
        long id = repository.saveAndFlush(item).getId();
        String body = "[" + id + "," + id + "]";
        mvc.perform(post("/api/quotes").header("Authorization", "Bearer user-token")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(190.02));
        mvc.perform(post("/api/quotes/self-invocation").header("Authorization", "Bearer user-token")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(190.019));
        mvc.perform(post("/api/quotes").header("Authorization", "Bearer user-token")
                        .contentType("application/json").content("[]"))
                .andExpect(status().isBadRequest());
    }

    private Jwt token(String value, String role) {
        return Jwt.withTokenValue(value).header("alg", "RS256").subject("test-user")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                .claim("realm_access", Map.of("roles", List.of(role))).build();
    }
}
