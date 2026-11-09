package net.montaki.demoescqrsaxon.commands.controllers;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.montaki.demoescqrsaxon.commands.dto.AmountRequest;
import net.montaki.demoescqrsaxon.commands.dto.CreateAccountRequest;
import net.montaki.demoescqrsaxon.common.commands.ActivateAccountCommand;
import net.montaki.demoescqrsaxon.common.commands.CreateAccountCommand;
import net.montaki.demoescqrsaxon.common.commands.CreditAccountCommand;
import net.montaki.demoescqrsaxon.common.commands.DebitAccountCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/commands/account")
public class AccountCommandController {
    private final CommandGateway commandGateway;
    public AccountCommandController(CommandGateway commandGateway) { this.commandGateway = commandGateway; }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<String> create(@Valid @RequestBody CreateAccountRequest request) {
        String id = UUID.randomUUID().toString();
        return commandGateway.send(new CreateAccountCommand(id, request.initialBalance(), request.currency().trim().toUpperCase()))
                .thenApply(ignored -> id);
    }
    @PostMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<Void> activate(@PathVariable String id) {
        return commandGateway.send(new ActivateAccountCommand(id));
    }
    @PostMapping("/{id}/credit")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<Void> credit(@PathVariable String id, @Valid @RequestBody AmountRequest request) {
        return commandGateway.send(new CreditAccountCommand(id, request.amount(), request.currency().trim().toUpperCase()));
    }
    @PostMapping("/{id}/debit")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<Void> debit(@PathVariable String id, @Valid @RequestBody AmountRequest request) {
        return commandGateway.send(new DebitAccountCommand(id, request.amount(), request.currency().trim().toUpperCase()));
    }
}
