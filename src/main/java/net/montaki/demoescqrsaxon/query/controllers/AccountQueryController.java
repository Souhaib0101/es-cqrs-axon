package net.montaki.demoescqrsaxon.query.controllers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.montaki.demoescqrsaxon.common.queries.FindAccountOperationsQuery;
import net.montaki.demoescqrsaxon.common.queries.FindAccountQuery;
import net.montaki.demoescqrsaxon.common.queries.FindAllAccountsQuery;
import net.montaki.demoescqrsaxon.common.queries.WatchAccountQuery;
import net.montaki.demoescqrsaxon.query.entities.AccountEntity;
import net.montaki.demoescqrsaxon.query.entities.AccountOperationEntity;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/queries")
public class AccountQueryController {
    private final QueryGateway queryGateway;
    public AccountQueryController(QueryGateway queryGateway) { this.queryGateway = queryGateway; }

    @GetMapping("/accounts")
    public CompletableFuture<List<AccountEntity>> allAccounts() {
        return queryGateway.query(new FindAllAccountsQuery(),
                org.axonframework.messaging.responsetypes.ResponseTypes.multipleInstancesOf(AccountEntity.class));
    }
    @GetMapping("/accounts/{id}")
    public CompletableFuture<AccountEntity> account(@PathVariable String id) {
        return queryGateway.query(new FindAccountQuery(id),
                org.axonframework.messaging.responsetypes.ResponseTypes.optionalInstanceOf(AccountEntity.class))
                .thenApply(result -> result.orElseThrow(() -> new AccountNotFoundException(id)));
    }
    @GetMapping("/accounts/{id}/operations")
    public CompletableFuture<List<AccountOperationEntity>> operations(@PathVariable String id) {
        return queryGateway.query(new FindAccountOperationsQuery(id),
                org.axonframework.messaging.responsetypes.ResponseTypes.multipleInstancesOf(AccountOperationEntity.class));
    }
    @GetMapping(value = "/accounts/{id}/watch", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter watch(@PathVariable String id) {
        SseEmitter emitter = new SseEmitter(0L);
        SubscriptionQueryResult<java.util.Optional<AccountEntity>, AccountOperationEntity> subscription =
                queryGateway.subscriptionQuery(new WatchAccountQuery(id),
                        org.axonframework.messaging.responsetypes.ResponseTypes.optionalInstanceOf(AccountEntity.class),
                        AccountOperationEntity.class);
        subscription.initialResult().whenComplete((initial, error) -> {
            if (error != null) { emitter.completeWithError(error); subscription.close(); return; }
            try { emitter.send(SseEmitter.event().name("account").data(initial.orElse(null))); }
            catch (IOException ex) { emitter.completeWithError(ex); subscription.close(); }
        });
        subscription.updates().subscribe(operation -> {
            try { emitter.send(SseEmitter.event().name("operation").data(operation)); }
            catch (IOException ex) { emitter.completeWithError(ex); subscription.close(); }
        }, error -> { emitter.completeWithError(error); subscription.close(); });
        emitter.onCompletion(subscription::close);
        emitter.onTimeout(subscription::close);
        return emitter;
    }
}
