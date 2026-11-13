package org.adventit.adapter.out.routes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import org.adventit.application.port.out.RoutePlanner;
import org.adventit.domain.trip.TravelMode;
import org.adventit.domain.trip.TripRoute;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GoogleRoutesRoutePlanner implements RoutePlanner {
    private static final String FIELD_MASK =
            "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline";

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
                Objects.requireNonNull(encodedPolyline, "Google route encoded polyline is missing"));
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

    private record GoogleRoute(String duration, Integer distanceMeters, GooglePolyline polyline) {
    }

    private record GooglePolyline(String encodedPolyline) {
    }
}
