package com.autoservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class OidcRolesTest {
    @Test
    void idTokenRolesGrantAdminAuthorityForUi() {
        var token = new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("sub", "manager", "realm_access", Map.of("roles", List.of("ADMIN", "USER"))));
        var result = new SecurityConfig().oidcAuthorities().mapAuthorities(List.of(new OidcUserAuthority(token)));
        assertThat(result).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "ROLE_USER", "OIDC_USER");
    }
}
