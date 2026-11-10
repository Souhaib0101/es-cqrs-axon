package net.montaki.demoescqrsaxon.events;

import net.montaki.demoescqrsaxon.enums.AccountStatus;
import java.math.BigDecimal;

public record AccountCreatedEvent(String accountId, BigDecimal initialBalance,
                                   String currency, AccountStatus status) {
}
