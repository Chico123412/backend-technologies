package com.autoservice.aspect;

import com.autoservice.dto.ServiceRequest;
import com.autoservice.entity.ServiceItem;
import com.autoservice.mapper.ServiceMapper;
import com.autoservice.repository.ServiceRepository;
import com.autoservice.service.CatalogService;
import com.autoservice.service.QuoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(CatalogAspectsIntegrationTest.Config.class)
class CatalogAspectsIntegrationTest {
    @Autowired QuoteService quotes;
    @Autowired CatalogService catalog;
    @Autowired ServiceRepository repository;

    @BeforeEach
    void setUp() {
        reset(repository);
    }

    @Test
    void externalCall_passesThroughProxyAndRoundsResult() {
        stubItem();
        assertThat(AopUtils.isAopProxy(quotes)).isTrue();
        assertThat(quotes.calculateTotal(List.of(1L, 1L))).isEqualTo(new BigDecimal("190.02"));
    }

    @Test
    void internalCall_toSameAnnotatedMethod_silentlySkipsRounding() {
        stubItem();
        assertThat(quotes.calculateTotalInternally(List.of(1L, 1L)))
                .isEqualTo(new BigDecimal("190.0190"));
    }

    @Test
    void externalCall_rejectsLargeBatchBeforeRepositoryAccess() {
        assertThatThrownBy(() -> quotes.calculateTotal(Collections.nCopies(11, 1L)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(repository);
    }

    @Test
    void internalCall_silentlySkipsBatchLimitToo() {
        stubItem();
        assertThat(quotes.calculateTotalInternally(Collections.nCopies(11, 1L)))
                .isEqualByComparingTo("1045.1045");
        verify(repository, times(11)).findById(1L);
    }

    @Test
    void readOnlyMode_blocksCreateUpdateAndDelete() {
        var request = new ServiceRequest("Огляд", "Огляд авто", new BigDecimal("100"), 30);
        assertBlocked(() -> catalog.create(request));
        assertBlocked(() -> catalog.update(1L, request));
        assertBlocked(() -> catalog.delete(1L));
        verifyNoInteractions(repository);
    }

    @Test
    void readOnlyMode_doesNotBlockReading() {
        stubItem();
        assertThat(catalog.findById(1L).price()).isEqualByComparingTo("100.01");
    }

    private void assertBlocked(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
    }

    private void stubItem() {
        var item = new ServiceItem();
        item.setId(1L);
        item.setName("Діагностика");
        item.setPrice(new BigDecimal("100.01"));
        item.setDurationMinutes(30);
        when(repository.findById(1L)).thenReturn(Optional.of(item));
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class Config {
        @Bean ServiceRepository repository() { return mock(ServiceRepository.class); }
        @Bean CatalogService catalog(ServiceRepository repository) {
            return new CatalogService(repository, Mappers.getMapper(ServiceMapper.class));
        }
        @Bean QuoteService quotes(CatalogService catalog) { return new QuoteService(catalog); }
        @Bean RoundMoneyAspect rounding() { return new RoundMoneyAspect(); }
        @Bean LimitBatchSizeAspect batchLimit() { return new LimitBatchSizeAspect(); }
        @Bean CatalogWritableAspect writable() { return new CatalogWritableAspect(true); }
    }
}
