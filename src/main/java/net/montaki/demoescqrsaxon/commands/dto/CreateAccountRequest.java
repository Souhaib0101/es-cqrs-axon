package net.montaki.demoescqrsaxon.commands.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotNull @DecimalMin("0.00") BigDecimal initialBalance,
        @NotBlank String currency) { }
