package com.autoservice.mapper;

import com.autoservice.dto.ServiceRequest;
import com.autoservice.dto.ServiceResponse;
import com.autoservice.entity.ServiceItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ServiceMapper {
    ServiceResponse toResponse(ServiceItem item);

    @Mapping(target = "id", ignore = true)
    ServiceItem toEntity(ServiceRequest request);

    @Mapping(target = "id", ignore = true)
    void update(ServiceRequest request, @MappingTarget ServiceItem item);
}
