package net.montaki.demoescqrsaxon.events;

import net.montaki.demoescqrsaxon.enums.AccountStatus;

public record AccountStatusUpdatedEvent(String accountId, AccountStatus status) {
}
