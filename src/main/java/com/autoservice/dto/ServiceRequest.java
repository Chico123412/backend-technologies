package com.autoservice.dto;

import com.autoservice.validation.ValidServiceDetails;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@ValidServiceDetails
public record ServiceRequest(
        @NotBlank(message = "Вкажіть назву") @Size(max = 120) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin(value = "0.01", message = "Ціна має бути додатною")
        @Digits(integer = 8, fraction = 2) BigDecimal price,
        @NotNull @Min(value = 1, message = "Мінімум 1 хвилина") @Max(1440) Integer durationMinutes
) {
}
