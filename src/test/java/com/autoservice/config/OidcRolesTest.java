package com.autoservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class OidcRolesTest {
    @Test
    void oidcAuthorities_realmRoles_addsRolesAndPreservesOriginalAuthority() {
        // Arrange
        var start = Instant.parse("2026-01-01T00:00:00Z");
        var token = new OidcIdToken("id-token", start, start.plusSeconds(300),
                Map.of("sub", "manager", "realm_access", Map.of("roles", List.of("ADMIN", "USER"))));
        var authority = new OidcUserAuthority(token);
        // Act
        var result = new SecurityConfig().oidcAuthorities().mapAuthorities(List.of(authority));
        // Assert
        assertThat(result.stream().anyMatch(value -> value == authority)).isTrue();
        assertThat(result).extracting(GrantedAuthority::getAuthority).contains("ROLE_ADMIN", "ROLE_USER", "OIDC_USER");
    }

    @Test
    void oidcAuthorities_regularAuthority_preservesWithoutAddingRoles() {
        // Arrange
        var original = new SimpleGrantedAuthority("SCOPE_profile");
        // Act
        var result = new SecurityConfig().oidcAuthorities().mapAuthorities(List.of(original));
        // Assert
        assertThat(result).extracting(GrantedAuthority::getAuthority).containsExactly("SCOPE_profile");
    }

    @Test
    void oidcAuthorities_emptyAuthorities_returnsEmptySet() {
        // Arrange / Act
        var result = new SecurityConfig().oidcAuthorities().mapAuthorities(List.of());
        // Assert
        assertThat(result).isEmpty();
    }
}
