package com.autoservice.controller;

import com.autoservice.config.WebConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import java.util.ArrayList;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProfileControllerTest {
    @Test
    void profile_usesRegisteredResolverToReturnCurrentUsername() throws Exception {
        var resolvers = new ArrayList<HandlerMethodArgumentResolver>();
        new WebConfig().addArgumentResolvers(resolvers);
        var mvc = MockMvcBuilders.standaloneSetup(new ProfileController())
                .setCustomArgumentResolvers(resolvers.toArray(HandlerMethodArgumentResolver[]::new)).build();
        var user = new UsernamePasswordAuthenticationToken("client", "", List.of());

        mvc.perform(get("/api/profile").principal(user)).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("client"));
    }
}
