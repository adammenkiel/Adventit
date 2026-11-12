package org.adventit.application.service;

import java.util.Arrays;
import java.util.List;
import org.adventit.application.port.in.PlanTripUseCase;
import org.adventit.application.port.out.RoutePlanner;
import org.adventit.domain.trip.TravelAlternative;
import org.adventit.domain.trip.TravelMode;
import org.adventit.domain.trip.TripPlan;
import org.springframework.stereotype.Service;

@Service
public class PlanTripService implements PlanTripUseCase {
    private final RoutePlanner routePlanner;

    public PlanTripService(RoutePlanner routePlanner) {
        this.routePlanner = routePlanner;
    }

    @Override
    public TripPlan planTrip(String startPlace, String endPlace) {
        List<TravelAlternative> alternatives = Arrays.stream(TravelMode.values())
                .map(mode -> new TravelAlternative(
                        mode,
                        routePlanner.findRoutes(startPlace, endPlace, mode)))
                .toList();
        return new TripPlan(startPlace, endPlace, alternatives);
    }
}
