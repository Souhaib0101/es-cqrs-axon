package net.montaki.demoescqrsaxon.commands;

import net.montaki.demoescqrsaxon.commands.aggregates.AccountAggregate;
import net.montaki.demoescqrsaxon.commands.commands.*;
import net.montaki.demoescqrsaxon.commands.exceptions.InsufficientBalanceException;
import net.montaki.demoescqrsaxon.enums.AccountStatus;
import net.montaki.demoescqrsaxon.events.*;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AccountAggregateTests {
    private AggregateTestFixture<AccountAggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(AccountAggregate.class);
    }

    @Test
    void createsAndActivatesAnAccount() {
        fixture.givenNoPriorActivity()
                .when(new CreateAccountCommand("a", new BigDecimal("100.00"), "MAD"))
                .expectSuccessfulHandlerExecution()
                .expectEvents(created("100.00"), new AccountActivatedEvent("a"));
    }

    @Test
    void replaysHistoryBeforeCheckingAvailableFunds() {
        fixture.given(created("100.00"), new AccountActivatedEvent("a"),
                        new AccountCreditedEvent("a", new BigDecimal("25.50"), "MAD"),
                        new AccountDebitedEvent("a", new BigDecimal("20.00"), "MAD"))
                .when(new DebitAccountCommand("a", new BigDecimal("105.50"), "MAD"))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new AccountDebitedEvent("a", new BigDecimal("105.50"), "MAD"))
                .expectState(account -> assertThat(account.getBalance()).isEqualByComparingTo("0"));
    }

    @Test
    void rejectsOverdraftEvenWhenTheBalanceIsZero() {
        fixture.given(created("0.00"), new AccountActivatedEvent("a"))
                .when(new DebitAccountCommand("a", BigDecimal.ONE, "MAD"))
                .expectException(InsufficientBalanceException.class)
                .expectNoEvents();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.001", "100000000000000000"})
    void rejectsInvalidAmounts(String amount) {
        fixture.given(created("100.00"), new AccountActivatedEvent("a"))
                .when(new CreditAccountCommand("a", new BigDecimal(amount), "MAD"))
                .expectException(IllegalArgumentException.class).expectNoEvents();
    }

    @Test
    void rejectsMismatchedCurrency() {
        fixture.given(created("100.00"), new AccountActivatedEvent("a"))
                .when(new DebitAccountCommand("a", BigDecimal.ONE, "EUR"))
                .expectException(IllegalArgumentException.class).expectNoEvents();
    }

    @Test
    void rejectsOperationsOnSuspendedAccounts() {
        fixture.given(created("100.00"), new AccountActivatedEvent("a"),
                        new AccountStatusUpdatedEvent("a", AccountStatus.SUSPENDED))
                .when(new CreditAccountCommand("a", BigDecimal.ONE, "MAD"))
                .expectException(IllegalStateException.class).expectNoEvents();
    }

    @Test
    void reactivatesASuspendedAccountWithoutRepeatingCreation() {
        fixture.given(created("100.00"), new AccountActivatedEvent("a"),
                        new AccountStatusUpdatedEvent("a", AccountStatus.SUSPENDED))
                .when(new UpdateAccountStatusCommand("a", AccountStatus.ACTIVATED))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new AccountStatusUpdatedEvent("a", AccountStatus.ACTIVATED));
    }

    private AccountCreatedEvent created(String balance) {
        return new AccountCreatedEvent("a", new BigDecimal(balance), "MAD", AccountStatus.CREATED);
    }
}
