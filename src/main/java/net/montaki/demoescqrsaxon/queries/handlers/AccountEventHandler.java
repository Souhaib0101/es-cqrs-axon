package net.montaki.demoescqrsaxon.queries.handlers;

import lombok.RequiredArgsConstructor;
import net.montaki.demoescqrsaxon.enums.*;
import net.montaki.demoescqrsaxon.events.*;
import net.montaki.demoescqrsaxon.queries.entities.*;
import net.montaki.demoescqrsaxon.queries.repository.*;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.*;
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

    @EventHandler
    public void on(AccountCreatedEvent event, EventMessage<?> message) {
        accounts.save(BankAccount.builder().id(event.accountId()).balance(event.initialBalance())
                .currency(event.currency()).status(event.status()).createdAt(message.getTimestamp()).build());
    }

    @EventHandler
    public void on(AccountActivatedEvent event) {
        account(event.accountId()).setStatus(AccountStatus.ACTIVATED);
    }

    @EventHandler
    public void on(AccountStatusUpdatedEvent event) {
        account(event.accountId()).setStatus(event.status());
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
