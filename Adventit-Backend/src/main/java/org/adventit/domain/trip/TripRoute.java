package org.adventit.domain.trip;

import java.util.Objects;

public record TripRoute(long durationSeconds, int distanceMeters, String encodedPolyline) {
    public TripRoute {
        if (durationSeconds < 0 || distanceMeters < 0) {
            throw new IllegalArgumentException("Route duration and distance must not be negative");
        }
        Objects.requireNonNull(encodedPolyline, "encodedPolyline");
    }
}
