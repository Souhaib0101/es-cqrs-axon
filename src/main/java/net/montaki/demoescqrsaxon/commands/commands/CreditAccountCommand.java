package net.montaki.demoescqrsaxon.commands.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import java.math.BigDecimal;

public record CreditAccountCommand(@TargetAggregateIdentifier String accountId,
                                   BigDecimal amount, String currency) {
}
