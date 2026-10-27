package com.autoservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ServiceDetailsValidator.class)
public @interface ValidServiceDetails {
    String message() default "Для послуги тривалістю понад 60 хвилин потрібно вказати опис";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
