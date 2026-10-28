package com.autoservice.aspect;

import com.autoservice.annotation.LimitBatchSize;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Aspect
@Component
@Order(100)
public class LimitBatchSizeAspect {
    @Around("@annotation(limit)")
    public Object checkSize(ProceedingJoinPoint point, LimitBatchSize limit) throws Throwable {
        Object[] args = point.getArgs();
        if (args.length == 0 || !(args[0] instanceof List<?> items)
                || items.isEmpty() || items.size() > limit.max() || items.stream().anyMatch(java.util.Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Потрібно передати від 1 до " + limit.max() + " послуг без порожніх елементів");
        }
        return point.proceed();
    }
}
