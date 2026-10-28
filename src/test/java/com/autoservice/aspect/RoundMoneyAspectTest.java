package com.autoservice.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoundMoneyAspectTest {
    private final ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
    private final RoundMoneyAspect aspect = new RoundMoneyAspect();

    @Test
    void fractionalResult_isRoundedHalfUp() throws Throwable {
        when(point.proceed()).thenReturn(new BigDecimal("190.0190"));
        assertThat(aspect.round(point)).isEqualTo(new BigDecimal("190.02"));
        verify(point).proceed();
    }

    @Test
    void midpoint_isRoundedUp() throws Throwable {
        when(point.proceed()).thenReturn(new BigDecimal("10.005"));
        assertThat(aspect.round(point)).isEqualTo(new BigDecimal("10.01"));
    }

    @Test
    void wholeAmount_hasTwoDecimalPlaces() throws Throwable {
        when(point.proceed()).thenReturn(BigDecimal.TEN);
        assertThat(aspect.round(point)).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void nullResult_isPreserved() throws Throwable {
        when(point.proceed()).thenReturn(null);
        assertThat(aspect.round(point)).isNull();
        verify(point).proceed();
    }

    @Test
    void targetException_isNotSwallowed() throws Throwable {
        var error = new IllegalStateException("Calculation failed");
        when(point.proceed()).thenThrow(error);
        assertThatThrownBy(() -> aspect.round(point)).isSameAs(error);
    }
}
