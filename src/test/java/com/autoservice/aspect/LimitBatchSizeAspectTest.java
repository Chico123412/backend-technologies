package com.autoservice.aspect;

import com.autoservice.annotation.LimitBatchSize;
import com.autoservice.service.QuoteService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LimitBatchSizeAspectTest {
    private final ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
    private final LimitBatchSizeAspect aspect = new LimitBatchSizeAspect();
    private final LimitBatchSize limit = limit();

    @Test
    void validBatch_callsProceedAndReturnsResult() throws Throwable {
        when(point.getArgs()).thenReturn(new Object[]{List.of(1L)});
        when(point.proceed()).thenReturn("result");
        assertThat(aspect.checkSize(point, limit)).isEqualTo("result");
        verify(point).proceed();
    }

    @Test
    void exactlyTenItems_areAllowed() throws Throwable {
        when(point.getArgs()).thenReturn(new Object[]{Collections.nCopies(10, 1L)});
        aspect.checkSize(point, limit);
        verify(point).proceed();
    }

    @Test
    void elevenItems_areRejectedBeforeTarget() throws Throwable {
        reject(new Object[]{Collections.nCopies(11, 1L)});
    }

    @Test
    void emptyList_isRejectedBeforeTarget() throws Throwable {
        reject(new Object[]{List.of()});
    }

    @Test
    void nullList_isRejectedBeforeTarget() throws Throwable {
        reject(new Object[]{null});
    }

    @Test
    void nullItem_isRejectedBeforeTarget() throws Throwable {
        reject(new Object[]{Arrays.asList(1L, null)});
    }

    @Test
    void wrongArgumentType_isRejectedBeforeTarget() throws Throwable {
        reject(new Object[]{"wrong"});
    }

    @Test
    void missingArgument_isRejectedBeforeTarget() throws Throwable {
        reject(new Object[]{});
    }

    @Test
    void targetException_isPropagated() throws Throwable {
        when(point.getArgs()).thenReturn(new Object[]{List.of(1L)});
        var error = new IllegalStateException("Unavailable");
        when(point.proceed()).thenThrow(error);
        assertThatThrownBy(() -> aspect.checkSize(point, limit)).isSameAs(error);
    }

    private void reject(Object[] args) throws Throwable {
        when(point.getArgs()).thenReturn(args);
        assertThatThrownBy(() -> aspect.checkSize(point, limit))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> {
                    assertThat(error.getStatusCode().value()).isEqualTo(400);
                    assertThat(error.getReason()).contains("від 1 до 10");
                });
        verify(point, never()).proceed();
    }

    private static LimitBatchSize limit() {
        try {
            return QuoteService.class.getMethod("calculateTotal", List.class).getAnnotation(LimitBatchSize.class);
        } catch (NoSuchMethodException error) {
            throw new AssertionError(error);
        }
    }
}
