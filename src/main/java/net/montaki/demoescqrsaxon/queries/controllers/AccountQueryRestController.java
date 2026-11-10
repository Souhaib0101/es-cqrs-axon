package net.montaki.demoescqrsaxon.queries.controllers;

import lombok.RequiredArgsConstructor;
import net.montaki.demoescqrsaxon.queries.dto.*;
import net.montaki.demoescqrsaxon.queries.queries.*;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/query/accounts")
@RequiredArgsConstructor
public class AccountQueryRestController {
    private final QueryGateway queryGateway;

    @GetMapping
    public CompletableFuture<List<BankAccountResponseDTO>> all() {
        return queryGateway.query(new GetAllAccountsQuery(), ResponseTypes.multipleInstancesOf(BankAccountResponseDTO.class));
    }

    @GetMapping("/{id}")
    public CompletableFuture<BankAccountResponseDTO> account(@PathVariable String id) {
        return queryGateway.query(new GetAccountQuery(id), BankAccountResponseDTO.class);
    }

    @GetMapping("/{id}/operations")
    public CompletableFuture<List<AccountOperationResponseDTO>> operations(@PathVariable String id) {
        return queryGateway.query(new GetAccountOperationsQuery(id),
                ResponseTypes.multipleInstancesOf(AccountOperationResponseDTO.class));
    }

    @GetMapping("/{id}/statement")
    public CompletableFuture<AccountStatementResponseDTO> statement(@PathVariable String id) {
        return queryGateway.query(new GetAccountStatementQuery(id), AccountStatementResponseDTO.class);
    }

    @GetMapping(value = "/{id}/watch", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<BankAccountResponseDTO> watch(@PathVariable String id) {
        return Flux.defer(() -> {
            var result = queryGateway.subscriptionQuery(new GetAccountQuery(id),
                    ResponseTypes.instanceOf(BankAccountResponseDTO.class),
                    ResponseTypes.instanceOf(BankAccountResponseDTO.class));
            return result.initialResult().concatWith(result.updates()).doFinally(signal -> result.close());
        });
    }
}
