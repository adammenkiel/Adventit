package org.adventit.adapter.out.routes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.adventit.application.port.out.RoutePlanner;
import org.adventit.domain.trip.NavigationInstruction;
import org.adventit.domain.trip.RouteLocation;
import org.adventit.domain.trip.RouteStep;
import org.adventit.domain.trip.TravelMode;
import org.adventit.domain.trip.TransitDetails;
import org.adventit.domain.trip.TransitLine;
import org.adventit.domain.trip.TransitStop;
import org.adventit.domain.trip.TripRoute;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GoogleRoutesRoutePlanner implements RoutePlanner {
    private static final String FIELD_MASK =
            "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline,"
                    + "routes.legs.steps.travelMode,routes.legs.steps.startLocation,"
                    + "routes.legs.steps.endLocation,routes.legs.steps.transitDetails,"
                    + "routes.legs.steps.navigationInstruction";

    private final RestClient restClient;
    private final String apiKey;
    private final String endpoint;

    public GoogleRoutesRoutePlanner(
            RestClient.Builder restClientBuilder,
            @Value("${app.google.routes.api-key:}") String apiKey,
            @Value("${app.google.routes.endpoint:https://routes.googleapis.com/directions/v2:computeRoutes}")
                    String endpoint) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    @Override
    public List<TripRoute> findRoutes(String startPlace, String endPlace, TravelMode travelMode) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Google Routes API key is not configured");
        }

        GoogleRoutesResponse response = restClient.post()
                .uri(endpoint)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .body(new GoogleRoutesRequest(
                        new GoogleWaypoint(startPlace),
                        new GoogleWaypoint(endPlace),
                        travelMode))
                .retrieve()
                .body(GoogleRoutesResponse.class);

        if (response == null || response.routes() == null) {
            return List.of();
        }
        return response.routes().stream()
                .map(GoogleRoutesRoutePlanner::toTripRoute)
                .toList();
    }

    private static TripRoute toTripRoute(GoogleRoute route) {
        String duration = Objects.requireNonNull(route.duration(), "Google route duration is missing");
        if (!duration.matches("\\d+(\\.\\d+)?s")) {
            throw new IllegalStateException("Google Routes API returned an invalid route duration");
        }
        long durationSeconds = new BigDecimal(duration.substring(0, duration.length() - 1))
                .setScale(0, RoundingMode.CEILING)
                .longValueExact();
        int distanceMeters =
                Objects.requireNonNull(route.distanceMeters(), "Google route distance is missing");
        String encodedPolyline = Objects.requireNonNull(
                route.polyline(), "Google route polyline is missing").encodedPolyline();
        return new TripRoute(
                durationSeconds,
                distanceMeters,
                Objects.requireNonNull(encodedPolyline, "Google route encoded polyline is missing"),
                route.legs() == null
                        ? List.of()
                        : route.legs().stream()
                                .filter(Objects::nonNull)
                                .filter(leg -> leg.steps() != null)
                                .flatMap(leg -> leg.steps().stream())
                                .filter(Objects::nonNull)
                                .map(GoogleRoutesRoutePlanner::toRouteStep)
                                .toList());
    }

    private static RouteStep toRouteStep(GoogleStep step) {
        return new RouteStep(
                step.travelMode(),
                toRouteLocation(step.startLocation()),
                toRouteLocation(step.endLocation()),
                step.navigationInstruction() == null
                        ? null
                        : new NavigationInstruction(
                                step.navigationInstruction().maneuver(),
                                step.navigationInstruction().instructions()),
                toTransitDetails(step.transitDetails()));
    }

    private static RouteLocation toRouteLocation(GoogleLocation location) {
        if (location == null || location.latLng() == null) {
            return null;
        }
        return new RouteLocation(location.latLng().latitude(), location.latLng().longitude());
    }

    private static TransitDetails toTransitDetails(GoogleTransitDetails details) {
        if (details == null) {
            return null;
        }
        GoogleTransitLine line = details.transitLine();
        return new TransitDetails(
                details.departureStop() == null ? null : new TransitStop(details.departureStop().name()),
                details.arrivalStop() == null ? null : new TransitStop(details.arrivalStop().name()),
                toInstant(details.departureTime()),
                toInstant(details.arrivalTime()),
                details.headsign(),
                line == null
                        ? null
                        : new TransitLine(
                                line.name(), line.vehicle() == null ? null : line.vehicle().type()));
    }

    private static Instant toInstant(String timestamp) {
        return timestamp == null ? null : Instant.parse(timestamp);
    }

    private record GoogleRoutesRequest(
            GoogleWaypoint origin,
            GoogleWaypoint destination,
            TravelMode travelMode) {
    }

    private record GoogleWaypoint(String address) {
    }

    private record GoogleRoutesResponse(List<GoogleRoute> routes) {
    }

    private record GoogleRoute(
            String duration, Integer distanceMeters, GooglePolyline polyline, List<GoogleLeg> legs) {
    }

    private record GooglePolyline(String encodedPolyline) {
    }

    private record GoogleLeg(List<GoogleStep> steps) {
    }

    private record GoogleStep(
            String travelMode,
            GoogleLocation startLocation,
            GoogleLocation endLocation,
            GoogleTransitDetails transitDetails,
            GoogleNavigationInstruction navigationInstruction) {
    }

    private record GoogleLocation(GoogleLatLng latLng) {
    }

    private record GoogleLatLng(double latitude, double longitude) {
    }

    private record GoogleNavigationInstruction(String maneuver, String instructions) {
    }

    private record GoogleTransitDetails(
            String arrivalTime,
            GoogleTransitStop arrivalStop,
            String departureTime,
            GoogleTransitStop departureStop,
            String headsign,
            GoogleTransitLine transitLine) {
    }

    private record GoogleTransitStop(String name) {
    }

    private record GoogleTransitLine(String name, GoogleVehicle vehicle) {
    }

    private record GoogleVehicle(String type) {
    }
}
