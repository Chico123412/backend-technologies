package com.autoservice.repository;

import com.autoservice.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface ServiceRepository extends JpaRepository<ServiceItem, Long> {
    @Query("select max(s.price) from ServiceItem s")
    BigDecimal findMaxPrice();

    @Query("select max(s.durationMinutes) from ServiceItem s")
    Integer findMaxDuration();
}
