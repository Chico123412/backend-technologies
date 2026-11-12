package com.autoservice.service;

import com.autoservice.dto.ServiceRequest;
import com.autoservice.entity.ServiceItem;
import com.autoservice.mapper.ServiceMapper;
import com.autoservice.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {
    @Mock ServiceRepository repository;
    @Spy ServiceMapper mapper = Mappers.getMapper(ServiceMapper.class);
    @InjectMocks CatalogService service;
    @Captor ArgumentCaptor<ServiceItem> itemCaptor;

    @Test
    void findAll_existingServices_returnsMappedListSortedById() {
        // Arrange
        var first = item(1L, "Діагностика", "300.00");
        var second = item(2L, "Заміна масла", "500.00");
        when(repository.findAll(Sort.by("id"))).thenReturn(List.of(first, second));
        // Act
        var result = service.findAll();
        // Assert
        assertThat(result).extracting("id").containsExactly(1L, 2L);
        assertThat(result).extracting("name").containsExactly("Діагностика", "Заміна масла");
        assertThat(result.get(1).price()).isEqualByComparingTo("500.00");
        verify(repository).findAll(Sort.by("id"));
    }

    @Test
    void findAll_emptyRepository_returnsEmptyList() {
        // Arrange
        when(repository.findAll(Sort.by("id"))).thenReturn(List.of());
        // Act
        var result = service.findAll();
        // Assert
        assertThat(result).isEmpty();
        verify(repository).findAll(Sort.by("id"));
    }

    @Test
    void findAll_repositoryFailure_propagatesException() {
        // Arrange
        var failure = new DataAccessResourceFailureException("Repository unavailable");
        when(repository.findAll(Sort.by("id"))).thenThrow(failure);
        // Act / Assert
        assertThatThrownBy(service::findAll).isSameAs(failure);
    }

    @Test
    void findById_existingService_returnsAllFields() {
        // Arrange
        when(repository.findById(7L)).thenReturn(Optional.of(item(7L, "Діагностика", "300.00")));
        // Act
        var result = service.findById(7L);
        // Assert
        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.name()).isEqualTo("Діагностика");
        assertThat(result.description()).isEqualTo("Опис послуги");
        assertThat(result.price()).isEqualByComparingTo("300.00");
        assertThat(result.durationMinutes()).isEqualTo(30);
    }

    @Test
    void findById_missingService_throws404WithReason() {
        // Arrange
        when(repository.findById(99L)).thenReturn(Optional.empty());
        // Act
        var error = catchThrowableOfType(ResponseStatusException.class, () -> service.findById(99L));
        // Assert
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(error.getReason()).isEqualTo("Послугу не знайдено");
        verify(repository, never()).save(any());
    }

    @Test
    void findById_repositoryFailure_propagatesException() {
        // Arrange
        var failure = new DataAccessResourceFailureException("Read failed");
        when(repository.findById(7L)).thenThrow(failure);
        // Act / Assert
        assertThatThrownBy(() -> service.findById(7L)).isSameAs(failure);
    }

    @Test
    void create_validRequest_savesMappedEntityAndReturnsAssignedId() {
        // Arrange
        var request = request("Заміна масла", "500.00", "Олива і фільтр");
        when(repository.save(any(ServiceItem.class))).thenAnswer(call -> {
            ServiceItem saved = call.getArgument(0);
            saved.setId(12L);
            return saved;
        });
        // Act
        var result = service.create(request);
        // Assert
        verify(repository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getName()).isEqualTo(request.name());
        assertThat(itemCaptor.getValue().getDescription()).isEqualTo("Олива і фільтр");
        assertThat(itemCaptor.getValue().getPrice()).isEqualByComparingTo("500.00");
        assertThat(itemCaptor.getValue().getDurationMinutes()).isEqualTo(45);
        assertThat(result.id()).isEqualTo(12L);
        assertThat(result.name()).isEqualTo(request.name());
    }

    @Test
    void create_optionalDescriptionMissing_preservesNullDescription() {
        // Arrange
        when(repository.save(any(ServiceItem.class))).thenAnswer(call -> call.getArgument(0));
        // Act
        var result = service.create(request("Діагностика", "0.01", null));
        // Assert
        assertThat(result.description()).isNull();
        assertThat(result.price()).isEqualByComparingTo("0.01");
        verify(repository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getDescription()).isNull();
    }

    @Test
    void create_repositoryRejectsEntity_propagatesException() {
        // Arrange
        var failure = new DataIntegrityViolationException("Save rejected");
        when(repository.save(any(ServiceItem.class))).thenThrow(failure);
        // Act / Assert
        assertThatThrownBy(() -> service.create(request("Послуга", "50.00", null))).isSameAs(failure);
        verify(repository).save(any(ServiceItem.class));
    }

    @Test
    void update_existingService_updatesFieldsAndSavesSameEntity() {
        // Arrange
        var existing = item(7L, "Стара назва", "100.00");
        var request = request("Нова назва", "650.00", "Новий опис");
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        // Act
        var result = service.update(7L, request);
        // Assert
        assertThat(result.name()).isEqualTo("Нова назва");
        assertThat(result.price()).isEqualByComparingTo("650.00");
        assertThat(result.description()).isEqualTo("Новий опис");
        assertThat(result.durationMinutes()).isEqualTo(45);
        assertThat(result.id()).isEqualTo(7L);
        verify(repository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue()).isSameAs(existing);
        assertThat(existing.getPrice()).isEqualByComparingTo("650.00");
        verify(mapper).update(request, existing);
    }

    @Test
    void update_missingService_throws404WithoutSaving() {
        // Arrange
        when(repository.findById(99L)).thenReturn(Optional.empty());
        // Act
        var error = catchThrowableOfType(ResponseStatusException.class,
                () -> service.update(99L, request("Нова", "50.00", null)));
        // Assert
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(error.getReason()).isEqualTo("Послугу не знайдено");
        verify(repository, never()).save(any());
        verify(mapper, never()).update(any(), any());
    }

    @Test
    void update_repositoryRejectsSave_propagatesException() {
        // Arrange
        var existing = item(7L, "Послуга", "100.00");
        var failure = new DataIntegrityViolationException("Update rejected");
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenThrow(failure);
        // Act / Assert
        assertThatThrownBy(() -> service.update(7L, request("Нова", "50.00", null))).isSameAs(failure);
    }

    @Test
    void delete_existingService_deletesExactlyLoadedEntity() {
        // Arrange
        var existing = item(7L, "Послуга", "100.00");
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        // Act
        service.delete(7L);
        // Assert
        verify(repository).delete(itemCaptor.capture());
        assertThat(itemCaptor.getValue()).isSameAs(existing);
        assertThat(itemCaptor.getValue().getId()).isEqualTo(7L);
        verify(repository, never()).save(any());
    }

    @Test
    void delete_missingService_throws404WithoutDeleting() {
        // Arrange
        when(repository.findById(99L)).thenReturn(Optional.empty());
        // Act
        var error = catchThrowableOfType(ResponseStatusException.class, () -> service.delete(99L));
        // Assert
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(error.getReason()).isEqualTo("Послугу не знайдено");
        verify(repository, never()).delete(any());
    }

    @Test
    void delete_repositoryFailure_propagatesException() {
        // Arrange
        var existing = item(7L, "Послуга", "100.00");
        var failure = new DataAccessResourceFailureException("Delete failed");
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        doThrow(failure).when(repository).delete(existing);
        // Act / Assert
        assertThatThrownBy(() -> service.delete(7L)).isSameAs(failure);
        verify(repository).delete(existing);
    }

    private static ServiceRequest request(String name, String price, String description) {
        return new ServiceRequest(name, description, new BigDecimal(price), 45);
    }

    private static ServiceItem item(Long id, String name, String price) {
        var item = new ServiceItem();
        item.setId(id);
        item.setName(name);
        item.setDescription("Опис послуги");
        item.setPrice(new BigDecimal(price));
        item.setDurationMinutes(30);
        return item;
    }
}
