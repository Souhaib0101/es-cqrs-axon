package net.montaki.demoescqrsaxon.commands.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AmountRequest(@NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal amount,
                            @NotBlank String currency) { }
