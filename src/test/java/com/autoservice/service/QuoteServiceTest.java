package com.autoservice.service;

import com.autoservice.dto.ServiceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {
    @Mock CatalogService catalog;
    @InjectMocks QuoteService quotes;

    @Test
    void oneService_hasNoDiscount() {
        when(catalog.findById(1L)).thenReturn(item("100.01"));
        assertThat(quotes.calculateTotal(List.of(1L))).isEqualByComparingTo("100.01");
        verify(catalog).findById(1L);
    }

    @Test
    void twoServices_receiveFivePercentDiscount() {
        when(catalog.findById(1L)).thenReturn(item("100.01"));
        when(catalog.findById(2L)).thenReturn(item("100.01"));
        assertThat(quotes.calculateTotal(List.of(1L, 2L))).isEqualByComparingTo("190.0190");
        verify(catalog).findById(1L);
        verify(catalog).findById(2L);
    }

    @Test
    void internalCall_returnsUnroundedValueWithoutProxy() {
        when(catalog.findById(1L)).thenReturn(item("100.01"));
        assertThat(quotes.calculateTotalInternally(List.of(1L, 1L))).isEqualByComparingTo("190.0190");
    }

    @Test
    void missingService_propagatesCatalogError() {
        var error = new IllegalArgumentException("Missing service");
        when(catalog.findById(99L)).thenThrow(error);
        assertThatThrownBy(() -> quotes.calculateTotal(List.of(99L))).isSameAs(error);
    }

    private ServiceResponse item(String price) {
        return new ServiceResponse(1L, "Діагностика", "Огляд", new BigDecimal(price), 30);
    }
}
