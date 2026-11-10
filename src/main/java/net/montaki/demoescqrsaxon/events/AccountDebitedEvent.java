package net.montaki.demoescqrsaxon.events;

import java.math.BigDecimal;

public record AccountDebitedEvent(String accountId, BigDecimal amount, String currency) {
}
