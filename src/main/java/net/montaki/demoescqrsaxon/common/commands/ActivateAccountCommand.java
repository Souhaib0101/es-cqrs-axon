package net.montaki.demoescqrsaxon.common.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record ActivateAccountCommand(@TargetAggregateIdentifier String id) { }
