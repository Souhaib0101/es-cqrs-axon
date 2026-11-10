package net.montaki.demoescqrsaxon.events;

import java.math.BigDecimal;

public record AccountCreditedEvent(String accountId, BigDecimal amount, String currency) {
}
