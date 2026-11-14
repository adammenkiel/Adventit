package org.adventit.domain.trip;

import java.time.Instant;

public record TransitDetails(
        TransitStop departureStop,
        TransitStop arrivalStop,
        Instant departureTime,
        Instant arrivalTime,
        String headsign,
        TransitLine transitLine) {
}
