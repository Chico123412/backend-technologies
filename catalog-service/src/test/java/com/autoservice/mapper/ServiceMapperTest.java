package com.autoservice.mapper;

import com.autoservice.dto.ServiceRequest;
import com.autoservice.entity.ServiceItem;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class ServiceMapperTest {
    private final ServiceMapper mapper = Mappers.getMapper(ServiceMapper.class);

    @Test
    void toEntity_validRequest_mapsAllFieldsWithoutId() {
        // Arrange
        var request = new ServiceRequest("Огляд", null, new BigDecimal("150.00"), 20);
        // Act
        var item = mapper.toEntity(request);
        // Assert
        assertThat(item.getId()).isNull();
        assertThat(item.getName()).isEqualTo("Огляд");
        assertThat(item.getDescription()).isNull();
        assertThat(item.getPrice()).isEqualByComparingTo("150.00");
        assertThat(item.getDurationMinutes()).isEqualTo(20);
    }

    @Test
    void update_existingEntity_preservesIdAndUpdatesData() {
        // Arrange
        var item = new ServiceItem();
        item.setId(7L);
        var request = new ServiceRequest("Огляд", "Опис", new BigDecimal("150.00"), 20);
        // Act
        mapper.update(request, item);
        var result = mapper.toResponse(item);
        // Assert
        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.name()).isEqualTo(request.name());
        assertThat(result.description()).isEqualTo(request.description());
        assertThat(result.price()).isEqualTo(request.price());
        assertThat(result.durationMinutes()).isEqualTo(request.durationMinutes());
    }

    @Test
    void mapping_nullSource_returnsNullOrPreservesTarget() {
        // Arrange
        var existing = new ServiceItem();
        existing.setId(7L);
        // Act
        var entity = mapper.toEntity(null);
        var response = mapper.toResponse(null);
        mapper.update(null, existing);
        // Assert
        assertThat(entity).isNull();
        assertThat(response).isNull();
        assertThat(existing.getId()).isEqualTo(7L);
    }
}
