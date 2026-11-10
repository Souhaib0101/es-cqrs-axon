package net.montaki.demoescqrsaxon.commands.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.montaki.demoescqrsaxon.commands.dto.*;
import net.montaki.demoescqrsaxon.commands.services.AccountCommandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/commands/accounts")
@RequiredArgsConstructor
public class AccountCommandRestController {
    private final AccountCommandService service;

    @PostMapping
    public CompletableFuture<ResponseEntity<Map<String, String>>> create(
            @Valid @RequestBody CreateAccountRequestDTO request) {
        return service.create(request).thenApply(id -> ResponseEntity
                .created(URI.create("/query/accounts/" + id)).body(Map.of("accountId", id)));
    }

    @PostMapping("/{id}/credit")
    public CompletableFuture<Void> credit(@PathVariable String id,
                                          @Valid @RequestBody AccountOperationRequestDTO request) {
        return service.credit(id, request);
    }

    @PostMapping("/{id}/debit")
    public CompletableFuture<Void> debit(@PathVariable String id,
                                         @Valid @RequestBody AccountOperationRequestDTO request) {
        return service.debit(id, request);
    }

    @PatchMapping("/{id}/status")
    public CompletableFuture<Void> updateStatus(@PathVariable String id,
                                                @Valid @RequestBody UpdateAccountStatusRequestDTO request) {
        return service.updateStatus(id, request);
    }
}
