package net.montaki.demoescqrsaxon.commands.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import java.math.BigDecimal;

public record CreateAccountCommand(@TargetAggregateIdentifier String accountId,
                                   BigDecimal initialBalance, String currency) {
}
