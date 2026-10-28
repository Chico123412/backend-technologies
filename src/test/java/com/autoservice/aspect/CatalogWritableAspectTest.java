package com.autoservice.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogWritableAspectTest {
    private final ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);

    @Test
    void normalMode_callsTargetAndReturnsItsResult() throws Throwable {
        when(point.proceed()).thenReturn("saved");
        assertThat(new CatalogWritableAspect(false).checkWritable(point)).isEqualTo("saved");
        verify(point).proceed();
    }

    @Test
    void readOnlyMode_returns503AndDoesNotCallTarget() throws Throwable {
        assertThatThrownBy(() -> new CatalogWritableAspect(true).checkWritable(point))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> {
                    assertThat(error.getStatusCode().value()).isEqualTo(503);
                    assertThat(error.getReason()).contains("лише для перегляду");
                });
        verify(point, never()).proceed();
    }

    @Test
    void targetException_isNotSwallowed() throws Throwable {
        var error = new IllegalStateException("Database unavailable");
        when(point.proceed()).thenThrow(error);
        assertThatThrownBy(() -> new CatalogWritableAspect(false).checkWritable(point)).isSameAs(error);
    }
}
