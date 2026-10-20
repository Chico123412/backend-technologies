package com.autoservice.repository;

import com.autoservice.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceItem, Long> {
}
