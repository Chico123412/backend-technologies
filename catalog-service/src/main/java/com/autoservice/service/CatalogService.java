package com.autoservice.service;

import com.autoservice.annotation.CatalogWritable;
import com.autoservice.dto.ServiceRequest;
import com.autoservice.dto.ServiceResponse;
import com.autoservice.entity.ServiceItem;
import com.autoservice.mapper.ServiceMapper;
import com.autoservice.messaging.CatalogEventOutbox;
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
    private final CatalogEventOutbox outbox;

    public CatalogService(ServiceRepository repository, ServiceMapper mapper, CatalogEventOutbox outbox) {
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
    }

    public List<ServiceResponse> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(mapper::toResponse).toList();
    }

    public ServiceResponse findById(long id) {
        return mapper.toResponse(findEntity(id));
    }

    @CatalogWritable
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceResponse create(ServiceRequest request) {
        ServiceResponse item = mapper.toResponse(repository.save(mapper.toEntity(request)));
        outbox.record("CREATED", item);
        return item;
    }

    @CatalogWritable
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceResponse update(long id, ServiceRequest request) {
        ServiceItem item = findEntity(id);
        mapper.update(request, item);
        ServiceResponse result = mapper.toResponse(repository.save(item));
        outbox.record("UPDATED", result);
        return result;
    }

    @CatalogWritable
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(long id) {
        ServiceItem item = findEntity(id);
        repository.delete(item);
        outbox.record("DELETED", mapper.toResponse(item));
    }

    private ServiceItem findEntity(long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Послугу не знайдено"));
    }
}
