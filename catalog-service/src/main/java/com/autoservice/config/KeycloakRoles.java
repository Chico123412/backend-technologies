package com.autoservice.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class KeycloakRoles {
    private KeycloakRoles() { }

    public static Collection<GrantedAuthority> fromClaims(Map<String, Object> claims) {
        Object realm = claims.get("realm_access");
        if (!(realm instanceof Map<?, ?> access) || !(access.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream().filter(String.class::isInstance)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }
}
