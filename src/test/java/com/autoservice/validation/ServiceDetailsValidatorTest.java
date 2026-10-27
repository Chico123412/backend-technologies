package com.autoservice.validation;

import com.autoservice.dto.ServiceRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class ServiceDetailsValidatorTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void longServiceWithDescription_isValid() {
        assertThat(validator.validate(request("Перевірка двигуна", 90))).isEmpty();
    }

    @Test
    void exactlyOneHourWithoutDescription_isValid() {
        assertThat(validator.validate(request(null, 60))).isEmpty();
    }

    @Test
    void shortServiceWithoutDescription_isValid() {
        assertThat(validator.validate(request("", 30))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void longServiceWithoutDescription_hasClearViolation(String description) {
        var violations = validator.validate(request(description, 61));

        assertThat(violations).singleElement().satisfies(violation -> {
            assertThat(violation.getConstraintDescriptor().getAnnotation()).isInstanceOf(ValidServiceDetails.class);
            assertThat(violation.getPropertyPath().toString()).isEqualTo("description");
            assertThat(violation.getMessage()).isEqualTo(
                    "Для послуги тривалістю понад 60 хвилин потрібно вказати опис");
        });
    }

    @Test
    void missingDuration_isHandledByNotNull() {
        var violations = validator.validate(request(null, null));
        assertThat(violations).singleElement()
                .satisfies(value -> assertThat(value.getPropertyPath().toString()).isEqualTo("durationMinutes"));
    }

    @Test
    void nullObject_doesNotAddBusinessViolation() {
        assertThat(new ServiceDetailsValidator().isValid(null, null)).isTrue();
    }

    private ServiceRequest request(String description, Integer duration) {
        return new ServiceRequest("Діагностика", description, new BigDecimal("300.00"), duration);
    }
}
