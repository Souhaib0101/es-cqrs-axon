package net.montaki.demoescqrsaxon.common.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;

public record CreateAccountCommand(@TargetAggregateIdentifier String id, BigDecimal initialBalance, String currency) { }
