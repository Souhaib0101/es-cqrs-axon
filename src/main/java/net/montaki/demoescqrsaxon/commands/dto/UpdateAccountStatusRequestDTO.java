package net.montaki.demoescqrsaxon.commands.dto;

import jakarta.validation.constraints.NotNull;
import net.montaki.demoescqrsaxon.enums.AccountStatus;

public record UpdateAccountStatusRequestDTO(@NotNull AccountStatus status) {
}
