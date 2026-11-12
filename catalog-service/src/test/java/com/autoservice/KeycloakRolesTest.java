package com.autoservice;

import com.autoservice.config.KeycloakRoles;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class KeycloakRolesTest {
    @Test
    void fromClaims_realmRoles_mapsSpringAuthorities() {
        // Arrange
        Map<String, Object> claims = Map.of("realm_access", Map.of("roles", List.of("ADMIN", "USER")));
        // Act
        var result = KeycloakRoles.fromClaims(claims);
        // Assert
        assertThat(result).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void fromClaims_missingRealm_returnsNoAuthorities() {
        // Arrange / Act
        var result = KeycloakRoles.fromClaims(Map.of());
        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void fromClaims_wrongRealmType_returnsNoAuthorities() {
        // Arrange / Act
        var result = KeycloakRoles.fromClaims(Map.of("realm_access", "ADMIN"));
        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void fromClaims_missingRoles_returnsNoAuthorities() {
        // Arrange / Act
        var result = KeycloakRoles.fromClaims(Map.of("realm_access", Map.of()));
        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void fromClaims_wrongRolesType_returnsNoAuthorities() {
        // Arrange / Act
        var result = KeycloakRoles.fromClaims(Map.of("realm_access", Map.of("roles", "ADMIN")));
        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void fromClaims_mixedRoles_ignoresNonStringValues() {
        // Arrange
        Map<String, Object> claims = Map.of("realm_access", Map.of("roles", List.of("USER", 123)));
        // Act
        var result = KeycloakRoles.fromClaims(claims);
        // Assert
        assertThat(result).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USER");
    }
}
