package net.montaki.demoescqrsaxon.commands.services;

import lombok.RequiredArgsConstructor;
import net.montaki.demoescqrsaxon.commands.commands.*;
import net.montaki.demoescqrsaxon.commands.dto.*;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AccountCommandService {
    private final CommandGateway commandGateway;

    public CompletableFuture<String> create(CreateAccountRequestDTO request) {
        String id = UUID.randomUUID().toString();
        return commandGateway.send(new CreateAccountCommand(id, request.initialBalance(), request.currency()))
                .thenApply(result -> id);
    }

    public CompletableFuture<Void> credit(String id, AccountOperationRequestDTO request) {
        return commandGateway.send(new CreditAccountCommand(id, request.amount(), request.currency()));
    }

    public CompletableFuture<Void> debit(String id, AccountOperationRequestDTO request) {
        return commandGateway.send(new DebitAccountCommand(id, request.amount(), request.currency()));
    }

    public CompletableFuture<Void> updateStatus(String id, UpdateAccountStatusRequestDTO request) {
        return commandGateway.send(new UpdateAccountStatusCommand(id, request.status()));
    }
}
