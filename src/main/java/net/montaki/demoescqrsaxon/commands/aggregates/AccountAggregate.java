package net.montaki.demoescqrsaxon.commands.aggregates;

import net.montaki.demoescqrsaxon.common.AccountStatus;
import net.montaki.demoescqrsaxon.common.commands.ActivateAccountCommand;
import net.montaki.demoescqrsaxon.common.commands.CreateAccountCommand;
import net.montaki.demoescqrsaxon.common.commands.CreditAccountCommand;
import net.montaki.demoescqrsaxon.common.commands.DebitAccountCommand;
import net.montaki.demoescqrsaxon.common.events.AccountActivatedEvent;
import net.montaki.demoescqrsaxon.common.events.AccountCreatedEvent;
import net.montaki.demoescqrsaxon.common.events.AccountCreditedEvent;
import net.montaki.demoescqrsaxon.common.events.AccountDebitedEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.math.BigDecimal;
import java.time.Instant;

@Aggregate
public class AccountAggregate {
    @AggregateIdentifier
    private String id;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;

    protected AccountAggregate() { }

    @CommandHandler
    public AccountAggregate(CreateAccountCommand command) {
        if (command.initialBalance().signum() < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        AggregateLifecycle.apply(new AccountCreatedEvent(command.id(), command.initialBalance(), command.currency()));
    }

    @CommandHandler
    public void handle(ActivateAccountCommand command) {
        if (status != AccountStatus.CREATED) {
            throw new IllegalStateException("Only a newly created account can be activated");
        }
        AggregateLifecycle.apply(new AccountActivatedEvent(command.id()));
    }

    @CommandHandler
    public void handle(CreditAccountCommand command) {
        ensureActive(command.currency());
        requirePositive(command.amount());
        AggregateLifecycle.apply(new AccountCreditedEvent(command.id(), command.amount(), currency, Instant.now()));
    }

    @CommandHandler
    public void handle(DebitAccountCommand command) {
        ensureActive(command.currency());
        requirePositive(command.amount());
        if (balance.compareTo(command.amount()) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        AggregateLifecycle.apply(new AccountDebitedEvent(command.id(), command.amount(), currency, Instant.now()));
    }

    private void ensureActive(String requestedCurrency) {
        if (status != AccountStatus.ACTIVATED) throw new IllegalStateException("Account is not active");
        if (!currency.equalsIgnoreCase(requestedCurrency)) throw new IllegalArgumentException("Currency does not match account");
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero");
    }

    @EventSourcingHandler
    public void on(AccountCreatedEvent event) {
        id = event.id(); balance = event.balance(); currency = event.currency(); status = AccountStatus.CREATED;
    }

    @EventSourcingHandler
    public void on(AccountActivatedEvent event) { status = AccountStatus.ACTIVATED; }

    @EventSourcingHandler
    public void on(AccountCreditedEvent event) { balance = balance.add(event.amount()); }

    @EventSourcingHandler
    public void on(AccountDebitedEvent event) { balance = balance.subtract(event.amount()); }
}
