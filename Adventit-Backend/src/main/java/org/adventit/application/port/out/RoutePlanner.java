package org.adventit.application.port.out;

import java.util.List;
import org.adventit.domain.trip.TravelMode;
import org.adventit.domain.trip.TripRoute;

public interface RoutePlanner {
    List<TripRoute> findRoutes(String startPlace, String endPlace, TravelMode travelMode);
}
