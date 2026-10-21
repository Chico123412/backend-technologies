package com.autoservice.controller;

import com.autoservice.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {
    @Mock ServiceRepository repository;
    @InjectMocks AdminController controller;

    @Test
    void summary_existingServices_returnsRepositoryCount() {
        // Arrange
        when(repository.count()).thenReturn(12L);
        // Act
        var result = controller.summary();
        // Assert
        assertThat(result).containsEntry("serviceCount", 12L).hasSize(1);
        verify(repository).count();
    }

    @Test
    void summary_emptyCatalog_returnsZero() {
        // Arrange
        when(repository.count()).thenReturn(0L);
        // Act
        var result = controller.summary();
        // Assert
        assertThat(result).containsEntry("serviceCount", 0L);
    }

    @Test
    void summary_repositoryFailure_propagatesException() {
        // Arrange
        var failure = new DataAccessResourceFailureException("Count failed");
        when(repository.count()).thenThrow(failure);
        // Act / Assert
        assertThatThrownBy(controller::summary).isSameAs(failure);
    }
}
