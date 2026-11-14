package org.adventit.domain.trip;

import java.util.List;
import java.util.Objects;

public record TripRoute(
        long durationSeconds, int distanceMeters, String encodedPolyline, List<RouteStep> steps) {
    public TripRoute {
        if (durationSeconds < 0 || distanceMeters < 0) {
            throw new IllegalArgumentException("Route duration and distance must not be negative");
        }
        Objects.requireNonNull(encodedPolyline, "encodedPolyline");
        steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
    }

    public TripRoute(long durationSeconds, int distanceMeters, String encodedPolyline) {
        this(durationSeconds, distanceMeters, encodedPolyline, List.of());
    }
}
