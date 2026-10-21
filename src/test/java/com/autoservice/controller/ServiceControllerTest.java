package com.autoservice.controller;

import com.autoservice.dto.ServiceRequest;
import com.autoservice.dto.ServiceResponse;
import com.autoservice.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceControllerTest {
    @Mock CatalogService service;
    @InjectMocks ServiceController controller;

    private final ServiceRequest request = new ServiceRequest("Діагностика", "Огляд", new BigDecimal("300.00"), 30);
    private final ServiceResponse response = new ServiceResponse(7L, "Діагностика", "Огляд", new BigDecimal("300.00"), 30);

    @Test
    void findAll_existingServices_returnsServiceResult() {
        // Arrange
        when(service.findAll()).thenReturn(List.of(response));
        // Act
        var result = controller.findAll();
        // Assert
        assertThat(result).containsExactly(response);
        verify(service).findAll();
    }

    @Test
    void findAll_emptyCatalog_returnsEmptyList() {
        // Arrange
        when(service.findAll()).thenReturn(List.of());
        // Act
        var result = controller.findAll();
        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findById_existingService_returnsServiceResult() {
        // Arrange
        when(service.findById(7L)).thenReturn(response);
        // Act
        var result = controller.findById(7L);
        // Assert
        assertThat(result).isEqualTo(response);
        verify(service).findById(7L);
    }

    @Test
    void findById_missingService_propagatesNotFound() {
        // Arrange
        var failure = new ResponseStatusException(HttpStatus.NOT_FOUND, "Послугу не знайдено");
        when(service.findById(99L)).thenThrow(failure);
        // Act / Assert
        assertThatThrownBy(() -> controller.findById(99L)).isSameAs(failure);
    }

    @Test
    void create_validRequest_returns201WithLocationAndBody() {
        // Arrange
        when(service.create(request)).thenReturn(response);
        // Act
        var result = controller.create(request);
        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getHeaders().getLocation()).isEqualTo(URI.create("/api/services/7"));
        assertThat(result.getBody()).isEqualTo(response);
        verify(service).create(request);
    }

    @Test
    void update_existingService_returnsUpdatedResult() {
        // Arrange
        when(service.update(7L, request)).thenReturn(response);
        // Act
        var result = controller.update(7L, request);
        // Assert
        assertThat(result).isEqualTo(response);
        verify(service).update(7L, request);
    }

    @Test
    void delete_existingService_returns204WithoutBody() {
        // Arrange / Act
        var result = controller.delete(7L);
        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
        verify(service).delete(7L);
    }

    @Test
    void delete_missingService_propagatesNotFound() {
        // Arrange
        var failure = new ResponseStatusException(HttpStatus.NOT_FOUND);
        doThrow(failure).when(service).delete(99L);
        // Act / Assert
        assertThatThrownBy(() -> controller.delete(99L)).isSameAs(failure);
        verify(service).delete(99L);
    }
}
