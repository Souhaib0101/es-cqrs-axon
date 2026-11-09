package net.montaki.demoescqrsaxon.query.services;

import java.util.List;
import java.util.Optional;
import net.montaki.demoescqrsaxon.common.AccountStatus;
import net.montaki.demoescqrsaxon.common.events.AccountActivatedEvent;
import net.montaki.demoescqrsaxon.common.events.AccountCreatedEvent;
import net.montaki.demoescqrsaxon.common.events.AccountCreditedEvent;
import net.montaki.demoescqrsaxon.common.events.AccountDebitedEvent;
import net.montaki.demoescqrsaxon.common.queries.FindAccountOperationsQuery;
import net.montaki.demoescqrsaxon.common.queries.FindAccountQuery;
import net.montaki.demoescqrsaxon.common.queries.FindAllAccountsQuery;
import net.montaki.demoescqrsaxon.common.queries.WatchAccountQuery;
import net.montaki.demoescqrsaxon.query.entities.AccountEntity;
import net.montaki.demoescqrsaxon.query.entities.AccountOperationEntity;
import net.montaki.demoescqrsaxon.query.repositories.AccountOperationRepository;
import net.montaki.demoescqrsaxon.query.repositories.AccountRepository;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.axonframework.queryhandling.SubscriptionQueryHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AccountProjection {
    private final AccountRepository accounts;
    private final AccountOperationRepository operations;
    private final QueryUpdateEmitter updateEmitter;

    public AccountProjection(AccountRepository accounts, AccountOperationRepository operations,
                             QueryUpdateEmitter updateEmitter) {
        this.accounts = accounts; this.operations = operations; this.updateEmitter = updateEmitter;
    }

    @EventHandler
    public void on(AccountCreatedEvent event) {
        accounts.save(new AccountEntity(event.id(), event.balance(), event.currency(), AccountStatus.CREATED));
    }
    @EventHandler
    public void on(AccountActivatedEvent event) {
        accounts.findById(event.id()).ifPresent(account -> { account.setStatus(AccountStatus.ACTIVATED); accounts.save(account); });
    }
    @EventHandler
    public void on(AccountCreditedEvent event) {
        accounts.findById(event.id()).ifPresent(account -> { account.setBalance(account.getBalance().add(event.amount())); accounts.save(account); });
        publishOperation(new AccountOperationEntity(event.id(), "CREDIT", event.amount(), event.currency(), event.occurredAt()));
    }
    @EventHandler
    public void on(AccountDebitedEvent event) {
        accounts.findById(event.id()).ifPresent(account -> { account.setBalance(account.getBalance().subtract(event.amount())); accounts.save(account); });
        publishOperation(new AccountOperationEntity(event.id(), "DEBIT", event.amount(), event.currency(), event.occurredAt()));
    }
    private void publishOperation(AccountOperationEntity operation) {
        AccountOperationEntity saved = operations.save(operation);
        updateEmitter.emit(WatchAccountQuery.class, query -> query.accountId().equals(saved.getAccountId()), saved);
    }

    @QueryHandler
    public Optional<AccountEntity> handle(FindAccountQuery query) { return accounts.findById(query.id()); }
    @QueryHandler
    public List<AccountEntity> handle(FindAllAccountsQuery query) { return accounts.findAll(); }
    @QueryHandler
    public List<AccountOperationEntity> handle(FindAccountOperationsQuery query) {
        return operations.findByAccountIdOrderByOccurredAtDesc(query.accountId());
    }
    @SubscriptionQueryHandler
    public Optional<AccountEntity> handle(WatchAccountQuery query) { return accounts.findById(query.accountId()); }
}
