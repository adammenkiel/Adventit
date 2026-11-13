package org.adventit.domain.trip;

import java.util.List;
import java.util.Objects;

public record TravelAlternative(TravelMode travelMode, List<TripRoute> routes) {
    public TravelAlternative {
        Objects.requireNonNull(travelMode, "travelMode");
        routes = List.copyOf(routes);
    }
}
