package net.montaki.demoescqrsaxon.queries.dto;

import java.util.List;

public record AccountStatementResponseDTO(BankAccountResponseDTO account,
                                          List<AccountOperationResponseDTO> operations) {
}
