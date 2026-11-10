package net.montaki.demoescqrsaxon.queries.handlers;

import lombok.RequiredArgsConstructor;
import net.montaki.demoescqrsaxon.enums.*;
import net.montaki.demoescqrsaxon.events.*;
import net.montaki.demoescqrsaxon.mappers.BankAccountMapper;
import net.montaki.demoescqrsaxon.queries.entities.*;
import net.montaki.demoescqrsaxon.queries.queries.GetAccountQuery;
import net.montaki.demoescqrsaxon.queries.repository.*;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.*;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Component
@ProcessingGroup("accounts")
@RequiredArgsConstructor
@Transactional
public class AccountEventHandler {
    private final BankAccountRepository accounts;
    private final AccountOperationRepository operations;
    private final BankAccountMapper mapper;
    private final QueryUpdateEmitter updates;

    @EventHandler
    public void on(AccountCreatedEvent event, EventMessage<?> message) {
        BankAccount account = accounts.save(BankAccount.builder().id(event.accountId()).balance(event.initialBalance())
                .currency(event.currency()).status(event.status()).createdAt(message.getTimestamp()).build());
        notifyWatchers(account, message);
    }

    @EventHandler
    public void on(AccountActivatedEvent event, EventMessage<?> message) {
        BankAccount account = account(event.accountId());
        account.setStatus(AccountStatus.ACTIVATED);
        notifyWatchers(account, message);
    }

    @EventHandler
    public void on(AccountStatusUpdatedEvent event, EventMessage<?> message) {
        BankAccount account = account(event.accountId());
        account.setStatus(event.status());
        notifyWatchers(account, message);
    }

    @EventHandler
    public void on(AccountCreditedEvent event, DomainEventMessage<?> message) {
        recordOperation(event.accountId(), event.amount(), event.currency(), OperationType.CREDIT, message);
    }

    @EventHandler
    public void on(AccountDebitedEvent event, DomainEventMessage<?> message) {
        recordOperation(event.accountId(), event.amount(), event.currency(), OperationType.DEBIT, message);
    }

    private void recordOperation(String id, BigDecimal amount, String currency, OperationType type,
                                 DomainEventMessage<?> message) {
        BankAccount account = account(id);
        account.setBalance(type == OperationType.CREDIT
                ? account.getBalance().add(amount) : account.getBalance().subtract(amount));
        operations.save(AccountOperation.builder().id(message.getIdentifier()).bankAccount(account)
                .amount(amount).currency(currency).type(type).operationDate(message.getTimestamp())
                .sequenceNumber(message.getSequenceNumber()).build());
        notifyWatchers(account, message);
    }

    private void notifyWatchers(BankAccount account, EventMessage<?> message) {
        if (!ReplayToken.isReplay(message)) {
            // Axon's emitter publishes after the event-processing unit of work commits.
            updates.emit(GetAccountQuery.class, query -> query.accountId().equals(account.getId()),
                    mapper.toDTO(account));
        }
    }

    @ResetHandler
    public void reset() {
        operations.deleteAllInBatch();
        accounts.deleteAllInBatch();
    }

    private BankAccount account(String id) {
        return accounts.findById(id).orElseThrow(() -> new NoSuchElementException("Account not found"));
    }
}
