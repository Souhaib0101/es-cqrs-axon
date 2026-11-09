package net.montaki.demoescqrsaxon.common.events;

import java.math.BigDecimal;

public record AccountCreatedEvent(String id, BigDecimal balance, String currency) { }
