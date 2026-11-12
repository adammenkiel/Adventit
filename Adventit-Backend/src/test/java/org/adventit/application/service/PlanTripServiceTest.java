package org.adventit.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.adventit.application.port.out.RoutePlanner;
import org.adventit.domain.trip.TravelMode;
import org.adventit.domain.trip.TripPlan;
import org.junit.jupiter.api.Test;

class PlanTripServiceTest {
    @Test
    void plansTransitAndWalkingRoutesForTheRequestedPlaces() {
        List<TravelMode> requestedModes = new ArrayList<>();
        RoutePlanner routePlanner = (startPlace, endPlace, travelMode) -> {
            assertEquals("Central Station", startPlace);
            assertEquals("City Museum", endPlace);
            requestedModes.add(travelMode);
            return List.of();
        };

        TripPlan tripPlan = new PlanTripService(routePlanner).planTrip("Central Station", "City Museum");

        assertEquals(List.of(TravelMode.TRANSIT, TravelMode.WALK), requestedModes);
        assertEquals("Central Station", tripPlan.startPlace());
        assertEquals("City Museum", tripPlan.endPlace());
        assertEquals(List.of(TravelMode.TRANSIT, TravelMode.WALK),
                tripPlan.alternatives().stream().map(alternative -> alternative.travelMode()).toList());
    }
}
