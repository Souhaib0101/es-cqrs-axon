package net.montaki.demoescqrsaxon.commands.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateAccountRequestDTO(
        @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal initialBalance,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency) {
}
