package org.adventit.domain.trip;

import java.util.List;
import java.util.Objects;

public record TripPlan(String startPlace, String endPlace, List<TravelAlternative> alternatives) {
    public TripPlan {
        Objects.requireNonNull(startPlace, "startPlace");
        Objects.requireNonNull(endPlace, "endPlace");
        alternatives = List.copyOf(alternatives);
    }
}
