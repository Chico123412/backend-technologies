package com.autoservice.annotation;

import com.autoservice.controller.AdminController;
import com.autoservice.controller.ServiceController;
import com.autoservice.repository.ServiceRepository;
import com.autoservice.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiControllerTest {
    @Test
    void composedController_matchesOriginalAnnotations() throws Exception {
        var repository = mock(ServiceRepository.class);
        when(repository.count()).thenReturn(3L);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminController(repository), new OriginalController()).build();

        var actual = mvc.perform(get("/api/admin/summary"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.serviceCount").value(3))
                .andReturn().getResponse();
        var original = mvc.perform(get("/original/summary"))
                .andExpect(status().isOk()).andReturn().getResponse();

        assertThat(actual.getContentAsString()).isEqualTo(original.getContentAsString());
        assertThat(actual.getContentType()).isEqualTo(original.getContentType());
        mvc.perform(post("/api/admin/summary")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void serviceController_keepsExistingRouteAndJsonResponse() throws Exception {
        var service = mock(CatalogService.class);
        when(service.findAll()).thenReturn(List.of());
        var mvc = MockMvcBuilders.standaloneSetup(new ServiceController(service)).build();

        mvc.perform(get("/api/services")).andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(service).findAll();
    }

    @RestController
    @RequestMapping("/original")
    public static class OriginalController {
        @GetMapping("/summary")
        public Map<String, Long> summary() {
            return Map.of("serviceCount", 3L);
        }
    }
}
