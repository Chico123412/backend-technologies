package com.autoservice;

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

    private Jwt token(String value, String role) {
        return Jwt.withTokenValue(value).header("alg", "RS256").subject("test-user")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                .claim("realm_access", Map.of("roles", List.of(role))).build();
    }
}
