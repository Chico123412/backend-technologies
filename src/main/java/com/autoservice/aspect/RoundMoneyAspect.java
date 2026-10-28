package com.autoservice.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Aspect
@Component
@Order(200)
public class RoundMoneyAspect {
    @Around("@annotation(com.autoservice.annotation.RoundMoney)")
    public Object round(ProceedingJoinPoint point) throws Throwable {
        BigDecimal result = (BigDecimal) point.proceed();
        return result == null ? null : result.setScale(2, RoundingMode.HALF_UP);
    }
}
