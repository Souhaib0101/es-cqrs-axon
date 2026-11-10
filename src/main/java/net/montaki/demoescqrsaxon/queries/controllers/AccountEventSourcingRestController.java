package net.montaki.demoescqrsaxon.queries.controllers;

import lombok.RequiredArgsConstructor;
import net.montaki.demoescqrsaxon.queries.dto.AccountEventResponseDTO;
import net.montaki.demoescqrsaxon.queries.services.EventSourcingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/query/accounts")
@RequiredArgsConstructor
public class AccountEventSourcingRestController {
    private final EventSourcingService service;

    @GetMapping("/{id}/events")
    public List<AccountEventResponseDTO> history(@PathVariable String id) {
        return service.history(id);
    }
}
