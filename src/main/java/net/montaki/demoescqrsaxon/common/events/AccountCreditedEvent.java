package net.montaki.demoescqrsaxon.common.events;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountCreditedEvent(String id, BigDecimal amount, String currency, Instant occurredAt) { }
