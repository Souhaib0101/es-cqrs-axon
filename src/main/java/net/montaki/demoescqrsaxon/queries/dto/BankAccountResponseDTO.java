package net.montaki.demoescqrsaxon.queries.dto;

import net.montaki.demoescqrsaxon.enums.AccountStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record BankAccountResponseDTO(String id, BigDecimal balance, String currency,
                                      AccountStatus status, Instant createdAt) {
}
