package com.autoservice.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
@Order(300)
public class CatalogWritableAspect {
    private final boolean readOnly;

    public CatalogWritableAspect(@Value("${autoservice.catalog.read-only:false}") boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Around("@annotation(com.autoservice.annotation.CatalogWritable)")
    public Object checkWritable(ProceedingJoinPoint point) throws Throwable {
        if (readOnly) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Каталог тимчасово доступний лише для перегляду");
        }
        return point.proceed();
    }
}
