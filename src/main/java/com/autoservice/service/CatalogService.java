package com.autoservice.service;

import com.autoservice.dto.ServiceRequest;
import com.autoservice.dto.ServiceResponse;
import com.autoservice.entity.ServiceItem;
import com.autoservice.mapper.ServiceMapper;
import com.autoservice.repository.ServiceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final ServiceRepository repository;
    private final ServiceMapper mapper;

    public CatalogService(ServiceRepository repository, ServiceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<ServiceResponse> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(mapper::toResponse).toList();
    }

    public ServiceResponse findById(long id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceResponse create(ServiceRequest request) {
        return mapper.toResponse(repository.save(mapper.toEntity(request)));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceResponse update(long id, ServiceRequest request) {
        ServiceItem item = findEntity(id);
        mapper.update(request, item);
        return mapper.toResponse(repository.save(item));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(long id) {
        repository.delete(findEntity(id));
    }

    private ServiceItem findEntity(long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Послугу не знайдено"));
    }
}
