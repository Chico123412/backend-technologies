package com.autoservice.validation;

import com.autoservice.dto.ServiceRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ServiceDetailsValidator implements ConstraintValidator<ValidServiceDetails, ServiceRequest> {
    @Override
    public boolean isValid(ServiceRequest request, ConstraintValidatorContext context) {
        if (request == null || request.durationMinutes() == null || request.durationMinutes() <= 60) {
            return true;
        }
        if (request.description() != null && !request.description().isBlank()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("description").addConstraintViolation();
        return false;
    }
}
