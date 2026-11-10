package net.montaki.demoescqrsaxon.commands.commands;

import net.montaki.demoescqrsaxon.enums.AccountStatus;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record UpdateAccountStatusCommand(@TargetAggregateIdentifier String accountId,
                                         AccountStatus status) {
}
