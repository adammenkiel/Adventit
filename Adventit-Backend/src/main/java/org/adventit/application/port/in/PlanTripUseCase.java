package org.adventit.application.port.in;

import org.adventit.domain.trip.TripPlan;

public interface PlanTripUseCase {
    TripPlan planTrip(String startPlace, String endPlace);
}
