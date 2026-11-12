package com.autoservice.controller;

import com.autoservice.service.QuoteService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuoteControllerTest {
    @Test
    void calculate_usesExternalCall() {
        var service = mock(QuoteService.class);
        when(service.calculateTotal(List.of(1L))).thenReturn(new BigDecimal("100.00"));
        assertThat(new QuoteController(service).calculate(List.of(1L))).containsEntry("total", new BigDecimal("100.00"));
        verify(service).calculateTotal(List.of(1L));
    }

    @Test
    void selfInvocation_usesDemonstrationMethod() {
        var service = mock(QuoteService.class);
        when(service.calculateTotalInternally(List.of(1L, 2L))).thenReturn(new BigDecimal("190.0190"));
        assertThat(new QuoteController(service).selfInvocation(List.of(1L, 2L)))
                .containsEntry("total", new BigDecimal("190.0190"));
        verify(service).calculateTotalInternally(List.of(1L, 2L));
    }
}
