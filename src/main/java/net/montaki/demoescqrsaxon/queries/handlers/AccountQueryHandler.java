package net.montaki.demoescqrsaxon.queries.handlers;

import lombok.RequiredArgsConstructor;
import net.montaki.demoescqrsaxon.mappers.BankAccountMapper;
import net.montaki.demoescqrsaxon.queries.dto.*;
import net.montaki.demoescqrsaxon.queries.queries.*;
import net.montaki.demoescqrsaxon.queries.repository.*;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountQueryHandler {
    private final BankAccountRepository accounts;
    private final AccountOperationRepository operations;
    private final BankAccountMapper mapper;

    @QueryHandler
    public List<BankAccountResponseDTO> handle(GetAllAccountsQuery query) {
        return accounts.findAll().stream().map(mapper::toDTO).toList();
    }

    @QueryHandler
    public BankAccountResponseDTO handle(GetAccountQuery query) {
        return mapper.toDTO(accounts.findById(query.accountId())
                .orElseThrow(() -> new NoSuchElementException("Account not found")));
    }

    @QueryHandler
    public List<AccountOperationResponseDTO> handle(GetAccountOperationsQuery query) {
        handle(new GetAccountQuery(query.accountId()));
        return operations.findByBankAccountIdOrderBySequenceNumberAsc(query.accountId())
                .stream().map(mapper::toDTO).toList();
    }

    @QueryHandler
    public AccountStatementResponseDTO handle(GetAccountStatementQuery query) {
        return new AccountStatementResponseDTO(handle(new GetAccountQuery(query.accountId())),
                handle(new GetAccountOperationsQuery(query.accountId())));
    }
}
