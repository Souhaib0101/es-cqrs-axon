package net.montaki.demoescqrsaxon.mappers;

import net.montaki.demoescqrsaxon.queries.dto.*;
import net.montaki.demoescqrsaxon.queries.entities.*;
import org.springframework.stereotype.Component;

@Component
public class BankAccountMapper {
    public BankAccountResponseDTO toDTO(BankAccount account) {
        return new BankAccountResponseDTO(account.getId(), account.getBalance(), account.getCurrency(),
                account.getStatus(), account.getCreatedAt());
    }

    public AccountOperationResponseDTO toDTO(AccountOperation operation) {
        return new AccountOperationResponseDTO(operation.getId(), operation.getBankAccount().getId(),
                operation.getOperationDate(), operation.getSequenceNumber(), operation.getAmount(),
                operation.getCurrency(), operation.getType());
    }
}
