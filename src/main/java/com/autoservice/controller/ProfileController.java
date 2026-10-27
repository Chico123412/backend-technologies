package com.autoservice.controller;

import com.autoservice.annotation.ApiController;
import com.autoservice.annotation.CurrentUsername;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Map;

@ApiController("/api/profile")
public class ProfileController {
    @GetMapping
    public Map<String, String> profile(@CurrentUsername String username) {
        return Map.of("username", username);
    }
}
