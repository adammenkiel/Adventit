package org.adventit.domain.trip;

public record RouteStep(
        String travelMode,
        RouteLocation startLocation,
        RouteLocation endLocation,
        NavigationInstruction navigationInstruction,
        TransitDetails transitDetails) {
}
