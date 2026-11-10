package net.montaki.demoescqrsaxon.commands.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AccountOperationRequestDTO(
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency) {
}
