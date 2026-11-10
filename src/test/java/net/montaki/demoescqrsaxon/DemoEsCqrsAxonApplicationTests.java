package net.montaki.demoescqrsaxon;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.montaki.demoescqrsaxon.enums.AccountStatus;
import net.montaki.demoescqrsaxon.queries.repository.BankAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;

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
