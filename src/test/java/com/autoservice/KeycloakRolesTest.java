package com.autoservice;

import com.autoservice.config.KeycloakRoles;
import com.autoservice.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRolesTest {
    @Test
    void mapsRealmRolesToSpringAuthorities() {
        assertThat(KeycloakRoles.fromClaims(Map.of("realm_access", Map.of("roles", List.of("ADMIN", "USER")))))
                .extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void missingRolesDoNotGrantAccess() {
        assertThat(KeycloakRoles.fromClaims(Map.of())).isEmpty();
    }
}
