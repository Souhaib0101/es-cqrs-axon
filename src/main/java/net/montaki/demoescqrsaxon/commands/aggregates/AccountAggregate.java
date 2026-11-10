package net.montaki.demoescqrsaxon.commands.aggregates;

import lombok.Getter;
import net.montaki.demoescqrsaxon.commands.commands.*;
import net.montaki.demoescqrsaxon.commands.exceptions.InsufficientBalanceException;
import net.montaki.demoescqrsaxon.enums.AccountStatus;
import net.montaki.demoescqrsaxon.events.*;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.math.BigDecimal;
import java.util.Currency;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
@Getter
public class AccountAggregate {
    private static final BigDecimal MAX_BALANCE = new BigDecimal("99999999999999999.99");

    @AggregateIdentifier
    private String accountId;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;

    protected AccountAggregate() {
        // Axon recreates state by replaying events through the sourcing handlers.
    }

    @CommandHandler
    public AccountAggregate(CreateAccountCommand command) {
        if (command.accountId() == null || command.accountId().isBlank()) {
            throw new IllegalArgumentException("Account ID is required");
        }
        validateMoney(command.initialBalance(), true);
        Currency.getInstance(command.currency());
        apply(new AccountCreatedEvent(command.accountId(), command.initialBalance(),
                command.currency(), AccountStatus.CREATED))
                .andThenApply(() -> new AccountActivatedEvent(command.accountId()));
    }

    @CommandHandler
    public void handle(CreditAccountCommand command) {
        validateOperation(command.amount(), command.currency());
        validateMoney(balance.add(command.amount()), true);
        apply(new AccountCreditedEvent(accountId, command.amount(), currency));
    }

    @CommandHandler
    public void handle(DebitAccountCommand command) {
        validateOperation(command.amount(), command.currency());
        if (balance.compareTo(command.amount()) < 0) {
            throw new InsufficientBalanceException();
        }
        apply(new AccountDebitedEvent(accountId, command.amount(), currency));
    }

    @CommandHandler
    public void handle(UpdateAccountStatusCommand command) {
        if (command.status() == null || command.status() == AccountStatus.CREATED) {
            throw new IllegalArgumentException("Status must be ACTIVATED or SUSPENDED");
        }
        if (status != command.status()) {
            apply(new AccountStatusUpdatedEvent(accountId, command.status()));
        }
    }

    @EventSourcingHandler
    public void on(AccountCreatedEvent event) {
        accountId = event.accountId();
        balance = event.initialBalance();
        currency = event.currency();
        status = event.status();
    }

    @EventSourcingHandler
    public void on(AccountActivatedEvent event) {
        status = AccountStatus.ACTIVATED;
    }

    @EventSourcingHandler
    public void on(AccountStatusUpdatedEvent event) {
        status = event.status();
    }

    @EventSourcingHandler
    public void on(AccountCreditedEvent event) {
        balance = balance.add(event.amount());
    }

    @EventSourcingHandler
    public void on(AccountDebitedEvent event) {
        balance = balance.subtract(event.amount());
    }

    private void validateOperation(BigDecimal amount, String operationCurrency) {
        if (status != AccountStatus.ACTIVATED) {
            throw new IllegalStateException("Account must be activated");
        }
        validateMoney(amount, false);
        if (!currency.equals(operationCurrency)) {
            throw new IllegalArgumentException("Currency must match the account currency");
        }
    }

    private static void validateMoney(BigDecimal amount, boolean allowZero) {
        if (amount == null || amount.signum() < 0 || (!allowZero && amount.signum() == 0)
                || amount.scale() > 2 || amount.compareTo(MAX_BALANCE) > 0) {
            throw new IllegalArgumentException("Amount must be " + (allowZero ? "non-negative" : "positive")
                    + " with at most 17 integer digits and 2 decimal places");
        }
    }
}
