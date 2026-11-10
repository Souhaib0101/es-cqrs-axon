package net.montaki.demoescqrsaxon;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.montaki.demoescqrsaxon.enums.AccountStatus;
import net.montaki.demoescqrsaxon.commands.commands.CreditAccountCommand;
import net.montaki.demoescqrsaxon.queries.controllers.AccountQueryRestController;
import net.montaki.demoescqrsaxon.queries.repository.AccountOperationRepository;
import net.montaki.demoescqrsaxon.queries.services.EventSourcingService;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.eventhandling.TrackingEventProcessor;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import net.montaki.demoescqrsaxon.queries.repository.BankAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.math.BigDecimal;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DemoEsCqrsAxonApplicationTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private BankAccountRepository accounts;
    @Autowired private AccountOperationRepository operations;
    @Autowired private AccountQueryRestController queries;
    @Autowired private CommandGateway commands;
    @Autowired private QueryUpdateEmitter updates;
    @Autowired private EventProcessingConfiguration processing;
    @Autowired private EventSourcingService eventSourcing;

    @Test
    void subscriptionSendsInitialStateAndOnlyUpdatesForTheSelectedAccount() throws Exception {
        String id = createAccount();
        String other = createAccount();
        int subscriptions = updates.activeSubscriptions().size();
        StepVerifier.create(queries.watch(id))
                .assertNext(account -> {
                    assertThat(account.id()).isEqualTo(id);
                    assertThat(account.balance()).isEqualByComparingTo("100");
                })
                .then(() -> commands.sendAndWait(new CreditAccountCommand(other, BigDecimal.ONE, "MAD")))
                .expectNoEvent(Duration.ofMillis(500))
                .then(() -> commands.sendAndWait(new CreditAccountCommand(id, new BigDecimal("2.50"), "MAD")))
                .assertNext(account -> {
                    assertThat(account.id()).isEqualTo(id);
                    assertThat(account.balance()).isEqualByComparingTo("102.50");
                })
                .thenCancel().verify(Duration.ofSeconds(10));
        assertThat(updates.activeSubscriptions()).hasSize(subscriptions);
    }

    @Test
    void rebuildsReadModelsFromPersistedEventsWithoutDuplicatingHistory() throws Exception {
        String id = createAccount();
        commands.sendAndWait(new CreditAccountCommand(id, new BigDecimal("5.25"), "MAD"));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(operations.findByBankAccountIdOrderBySequenceNumberAsc(id)).hasSize(1));
        var history = eventSourcing.history(id);
        var operation = operations.findByBankAccountIdOrderBySequenceNumberAsc(id).getFirst();
        var processor = processing.eventProcessor("accounts", TrackingEventProcessor.class).orElseThrow();
        processor.shutDown();
        try {
            processor.resetTokens();
            assertThat(accounts.findAll()).isEmpty();
        } finally {
            processor.start();
        }
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(accounts.findById(id).orElseThrow(() -> new AssertionError("Replay not finished"))
                    .getBalance()).isEqualByComparingTo("105.25");
            assertThat(operations.findByBankAccountIdOrderBySequenceNumberAsc(id)).hasSize(1);
        });
        var replayed = operations.findByBankAccountIdOrderBySequenceNumberAsc(id).getFirst();
        assertThat(replayed.getId()).isEqualTo(operation.getId());
        assertThat(replayed.getOperationDate()).isEqualTo(operation.getOperationDate());
        assertThat(eventSourcing.history(id)).isEqualTo(history);
    }

    @Test
    void commandsProduceEventsAndUpdateTheSeparateReadModel() throws Exception {
        String id = createAccount();
        perform(post("/commands/accounts/{id}/credit", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":50.25,\"currency\":\"MAD\"}"))
                .andExpect(status().isOk());
        perform(post("/commands/accounts/{id}/debit", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":20.10,\"currency\":\"MAD\"}"))
                .andExpect(status().isOk());

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(accounts.findById(id).orElseThrow(() -> new AssertionError("Projection not ready"))
                        .getBalance()).isEqualByComparingTo("130.15"));
        perform(get("/query/accounts/{id}/statement", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.balance").value(130.15))
                .andExpect(jsonPath("$.account.currency").value("MAD"))
                .andExpect(jsonPath("$.account.status").value("ACTIVATED"))
                .andExpect(jsonPath("$.operations.length()").value(2))
                .andExpect(jsonPath("$.operations[0].type").value("CREDIT"))
                .andExpect(jsonPath("$.operations[1].type").value("DEBIT"));
        perform(get("/query/accounts/{id}/events", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].type").value("AccountCreatedEvent"))
                .andExpect(jsonPath("$[1].type").value("AccountActivatedEvent"))
                .andExpect(jsonPath("$[3].sequenceNumber").value(3));
    }

    @Test
    void rejectsInvalidRequestsAndOverdraftsWithoutAppendingEvents() throws Exception {
        perform(post("/commands/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"initialBalance\":-1,\"currency\":\"MAD\"}"))
                .andExpect(status().isBadRequest());
        String id = createAccount();
        perform(post("/commands/accounts/{id}/debit", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":101,\"currency\":\"MAD\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Insufficient balance"));
        perform(post("/commands/accounts/{id}/credit", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":1,\"currency\":\"EUR\"}"))
                .andExpect(status().isBadRequest());
        perform(get("/query/accounts/{id}/events", id))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void statusChangesAreProjectedAndSuspendMoneyOperations() throws Exception {
        String id = createAccount();
        perform(patch("/commands/accounts/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk());
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(accounts.findById(id).orElseThrow(() -> new AssertionError("Projection not ready"))
                        .getStatus()).isEqualTo(AccountStatus.SUSPENDED));
        perform(post("/commands/accounts/{id}/credit", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":1,\"currency\":\"MAD\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownAccountsReturnNotFoundOnBothSides() throws Exception {
        perform(get("/query/accounts/missing")).andExpect(status().isNotFound());
        perform(get("/query/accounts/missing/operations")).andExpect(status().isNotFound());
        perform(get("/query/accounts/missing/events")).andExpect(status().isNotFound());
        perform(post("/commands/accounts/missing/credit").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":1,\"currency\":\"MAD\"}"))
                .andExpect(status().isNotFound());
    }

    private String createAccount() throws Exception {
        var result = perform(post("/commands/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"initialBalance\":100,\"currency\":\"MAD\"}"))
                .andExpect(status().isCreated()).andReturn();
        String id = json.readTree(result.getResponse().getContentAsString()).get("accountId").asText();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(accounts.findById(id).orElseThrow(() -> new AssertionError("Projection not ready"))
                        .getStatus()).isEqualTo(AccountStatus.ACTIVATED));
        return id;
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        var action = mvc.perform(request);
        MvcResult result = action.andReturn();
        if (result.getRequest().isAsyncStarted()) {
            result.getAsyncResult(10000);
            return mvc.perform(asyncDispatch(result));
        }
        return action;
    }
}
