package net.montaki.demoescqrsaxon.queries.dto;

import net.montaki.demoescqrsaxon.enums.OperationType;
import java.math.BigDecimal;
import java.time.Instant;

public record AccountOperationResponseDTO(String id, String accountId, Instant operationDate,
                                          long sequenceNumber, BigDecimal amount, String currency,
                                          OperationType type) {
}
