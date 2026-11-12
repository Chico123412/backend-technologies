package com.autoservice.web;

import com.autoservice.annotation.CurrentUsername;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class CurrentUsernameResolverTest {
    private final CurrentUsernameResolver resolver = new CurrentUsernameResolver();

    @Test
    void supportsParameter_acceptsOnlyAnnotatedString() throws Exception {
        assertThat(resolver.supportsParameter(parameter(0))).isTrue();
        assertThat(resolver.supportsParameter(parameter(1))).isFalse();
        assertThat(resolver.supportsParameter(parameter(2))).isFalse();
    }

    @Test
    void jwt_returnsPreferredUsernameInsteadOfSubject() throws Exception {
        var jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("user-id")
                .claim("preferred_username", "client").build();
        assertThat(resolve(new JwtAuthenticationToken(jwt, List.of()))).isEqualTo("client");
    }

    @Test
    void oauthUser_returnsPreferredUsername() throws Exception {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var user = new DefaultOAuth2User(authorities,
                Map.of("sub", "user-id", "preferred_username", "manager"), "sub");
        assertThat(resolve(new OAuth2AuthenticationToken(user, authorities, "keycloak"))).isEqualTo("manager");
    }

    @Test
    void missingPreferredUsername_returnsAuthenticationName() throws Exception {
        var jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("user-id").build();
        assertThat(resolve(new JwtAuthenticationToken(jwt, List.of()))).isEqualTo("user-id");
    }

    @Test
    void blankPreferredUsername_returnsAuthenticationName() throws Exception {
        var jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("user-id")
                .claim("preferred_username", " ").build();
        assertThat(resolve(new JwtAuthenticationToken(jwt, List.of()))).isEqualTo("user-id");
    }

    @Test
    void regularAuthentication_returnsName() throws Exception {
        assertThat(resolve(new UsernamePasswordAuthenticationToken("client", "", List.of()))).isEqualTo("client");
    }

    @Test
    void missingAuthentication_returns401() {
        assertUnauthorized(null);
    }

    @Test
    void anonymousAuthentication_returns401() {
        assertUnauthorized(new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    }

    @Test
    void unauthenticatedToken_returns401() {
        assertUnauthorized(new UsernamePasswordAuthenticationToken("client", "password"));
    }

    private void assertUnauthorized(Authentication authentication) {
        assertThatThrownBy(() -> resolve(authentication)).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode().value()).isEqualTo(401));
    }

    private String resolve(Authentication authentication) throws Exception {
        var request = new MockHttpServletRequest();
        request.setUserPrincipal(authentication);
        return resolver.resolveArgument(parameter(0), null, new ServletWebRequest(request), null);
    }

    private MethodParameter parameter(int index) throws Exception {
        return new MethodParameter(getClass().getDeclaredMethod("example", String.class, String.class, Long.class), index);
    }

    private void example(@CurrentUsername String username, String plain, @CurrentUsername Long wrongType) {
    }
}
