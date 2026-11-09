package net.montaki.demoescqrsaxon.common.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;

public record DebitAccountCommand(@TargetAggregateIdentifier String id, BigDecimal amount, String currency) { }
