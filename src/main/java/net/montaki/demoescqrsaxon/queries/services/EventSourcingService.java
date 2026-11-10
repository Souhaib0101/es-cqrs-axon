package net.montaki.demoescqrsaxon.queries.services;

import lombok.RequiredArgsConstructor;
import net.montaki.demoescqrsaxon.queries.dto.AccountEventResponseDTO;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class EventSourcingService {
    private final EventStore eventStore;

    public List<AccountEventResponseDTO> history(String accountId) {
        var events = eventStore.readEvents(accountId).asStream()
                .map(event -> new AccountEventResponseDTO(event.getIdentifier(), event.getSequenceNumber(),
                        event.getTimestamp(), event.getPayloadType().getSimpleName(), event.getPayload()))
                .toList();
        if (events.isEmpty()) {
            throw new NoSuchElementException("Account not found");
        }
        return events;
    }
}
