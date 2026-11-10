package net.montaki.demoescqrsaxon.queries.dto;

import java.time.Instant;

public record AccountEventResponseDTO(String eventId, long sequenceNumber, Instant timestamp,
                                      String type, Object payload) {
}
