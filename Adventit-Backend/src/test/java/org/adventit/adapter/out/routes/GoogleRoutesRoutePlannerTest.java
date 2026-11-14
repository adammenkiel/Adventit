package org.adventit.adapter.out.routes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.adventit.domain.trip.NavigationInstruction;
import org.adventit.domain.trip.RouteLocation;
import org.adventit.domain.trip.RouteStep;
import org.adventit.domain.trip.TravelMode;
import org.adventit.domain.trip.TransitDetails;
import org.adventit.domain.trip.TransitLine;
import org.adventit.domain.trip.TransitStop;
import org.adventit.domain.trip.TripRoute;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleRoutesRoutePlannerTest {
    private static final String ENDPOINT = "https://routes.googleapis.com/directions/v2:computeRoutes";
    private static final String FIELD_MASK = "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline,"
            + "routes.legs.steps.travelMode,routes.legs.steps.startLocation,routes.legs.steps.endLocation,"
            + "routes.legs.steps.transitDetails,routes.legs.steps.navigationInstruction";

    @Test
    void sendsGoogleRoutesRequestAndMapsRouteResponse() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "test-api-key"))
                .andExpect(header(
                        "X-Goog-FieldMask",
                        FIELD_MASK))
                .andExpect(content().json("""
                        {
                          "origin": {"address": "Central Station"},
                          "destination": {"address": "City Museum"},
                          "travelMode": "TRANSIT"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "routes": [{
                            "duration": "123.4s",
                            "distanceMeters": 1200,
                            "polyline": {"encodedPolyline": "abc"},
                            "legs": [{"steps": [{
                              "travelMode": "TRANSIT",
                              "startLocation": {"latLng": {"latitude": 51.5, "longitude": -0.1}},
                              "endLocation": {"latLng": {"latitude": 51.6, "longitude": -0.2}},
                              "navigationInstruction": {
                                "maneuver": "DEPART",
                                "instructions": "Take the train"
                              },
                              "transitDetails": {
                                "departureStop": {"name": "Central Station"},
                                "arrivalStop": {"name": "City Museum"},
                                "departureTime": "2026-11-14T10:00:00Z",
                                "arrivalTime": "2026-11-14T10:10:00Z",
                                "headsign": "City Centre",
                                "transitLine": {
                                  "name": "Blue Line",
                                  "vehicle": {"type": "SUBWAY"}
                                }
                              }
                            }]}]
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));
        GoogleRoutesRoutePlanner routePlanner =
                new GoogleRoutesRoutePlanner(restClientBuilder, "test-api-key", ENDPOINT);

        List<TripRoute> routes = routePlanner.findRoutes(
                "Central Station",
                "City Museum",
                TravelMode.TRANSIT);

        assertEquals(List.of(new TripRoute(
                124,
                1200,
                "abc",
                List.of(new RouteStep(
                        "TRANSIT",
                        new RouteLocation(51.5, -0.1),
                        new RouteLocation(51.6, -0.2),
                        new NavigationInstruction("DEPART", "Take the train"),
                        new TransitDetails(
                                new TransitStop("Central Station"),
                                new TransitStop("City Museum"),
                                java.time.Instant.parse("2026-11-14T10:00:00Z"),
                                java.time.Instant.parse("2026-11-14T10:10:00Z"),
                                "City Centre",
                                new TransitLine("Blue Line", "SUBWAY")))))), routes);
        server.verify();
    }

    @Test
    void mapsMultipleTransitStepsAndTransferInOrder() {
        GoogleRoutesRoutePlanner routePlanner = routePlannerFor("""
                {
                  "routes": [{
                    "duration": "1800s",
                    "distanceMeters": 5000,
                    "polyline": {"encodedPolyline": "journey"},
                    "legs": [{"steps": [
                      {
                        "travelMode": "TRANSIT",
                        "transitDetails": {
                          "departureStop": {"name": "Start"},
                          "arrivalStop": {"name": "First interchange"},
                          "departureTime": "2026-11-14T10:00:00Z",
                          "arrivalTime": "2026-11-14T10:10:00Z",
                          "headsign": "North",
                          "transitLine": {"name": "Line A", "vehicle": {"type": "BUS"}}
                        }
                      },
                      {
                        "travelMode": "WALK",
                        "navigationInstruction": {
                          "maneuver": "TURN_LEFT",
                          "instructions": "Walk to the other platform"
                        }
                      },
                      {
                        "travelMode": "TRANSIT",
                        "transitDetails": {
                          "departureStop": {"name": "Second interchange"},
                          "arrivalStop": {"name": "Destination"},
                          "departureTime": "2026-11-14T10:15:00Z",
                          "arrivalTime": "2026-11-14T10:30:00Z",
                          "headsign": "South",
                          "transitLine": {"name": "Line B", "vehicle": {"type": "SUBWAY"}}
                        }
                      }
                    ]}]
                  }]
                }
                """);

        TripRoute route = routePlanner.findRoutes("Start", "Destination", TravelMode.TRANSIT).get(0);

        assertEquals(3, route.steps().size());
        assertEquals("TRANSIT", route.steps().get(0).travelMode());
        assertEquals("First interchange", route.steps().get(0).transitDetails().arrivalStop().name());
        assertEquals("WALK", route.steps().get(1).travelMode());
        assertEquals(
                "Walk to the other platform",
                route.steps().get(1).navigationInstruction().instructions());
        assertEquals("TRANSIT", route.steps().get(2).travelMode());
        assertEquals("Line B", route.steps().get(2).transitDetails().transitLine().name());
        assertEquals("SUBWAY", route.steps().get(2).transitDetails().transitLine().vehicleType());
        assertEquals(1800, route.durationSeconds());
        assertEquals(5000, route.distanceMeters());
        assertEquals("journey", route.encodedPolyline());
    }

    @Test
    void mapsOneTransitStepWithStopsTimesLineVehicleAndHeadsign() {
        GoogleRoutesRoutePlanner routePlanner = routePlannerFor("""
                {
                  "routes": [{
                    "duration": "600s",
                    "distanceMeters": 2000,
                    "polyline": {"encodedPolyline": "single"},
                    "legs": [{"steps": [{
                      "travelMode": "TRANSIT",
                      "transitDetails": {
                        "departureStop": {"name": "Start"},
                        "arrivalStop": {"name": "End"},
                        "departureTime": "2026-11-14T10:00:00+01:00",
                        "arrivalTime": "2026-11-14T10:10:00+01:00",
                        "headsign": "Airport",
                        "transitLine": {"name": "Express", "vehicle": {"type": "RAIL"}}
                      }
                    }]}]
                  }]
                }
                """);

        RouteStep step = routePlanner.findRoutes("Start", "End", TravelMode.TRANSIT)
                .get(0).steps().get(0);

        assertEquals(new TransitDetails(
                new TransitStop("Start"),
                new TransitStop("End"),
                java.time.Instant.parse("2026-11-14T09:00:00Z"),
                java.time.Instant.parse("2026-11-14T09:10:00Z"),
                "Airport",
                new TransitLine("Express", "RAIL")), step.transitDetails());
    }

    @Test
    void mapsWalkingStepWithoutTransitDetails() {
        GoogleRoutesRoutePlanner routePlanner = routePlannerFor("""
                {
                  "routes": [{
                    "duration": "300s",
                    "distanceMeters": 400,
                    "polyline": {"encodedPolyline": "walk"},
                    "legs": [{"steps": [{
                      "travelMode": "WALK",
                      "startLocation": {"latLng": {"latitude": 1.0, "longitude": 2.0}},
                      "endLocation": {"latLng": {"latitude": 3.0, "longitude": 4.0}},
                      "navigationInstruction": {
                        "maneuver": "TURN_RIGHT",
                        "instructions": "Turn right"
                      }
                    }]}]
                  }]
                }
                """);

        RouteStep step = routePlanner.findRoutes("Start", "End", TravelMode.WALK)
                .get(0).steps().get(0);

        assertEquals("WALK", step.travelMode());
        assertEquals(new RouteLocation(1.0, 2.0), step.startLocation());
        assertEquals(new RouteLocation(3.0, 4.0), step.endLocation());
        assertEquals(new NavigationInstruction("TURN_RIGHT", "Turn right"), step.navigationInstruction());
        assertNull(step.transitDetails());
    }

    @Test
    void mapsTransitStepWhenOptionalTransitDetailsAreMissing() {
        GoogleRoutesRoutePlanner routePlanner = routePlannerFor("""
                {
                  "routes": [{
                    "duration": "60s",
                    "distanceMeters": 100,
                    "polyline": {"encodedPolyline": "partial"},
                    "legs": [{"steps": [{"travelMode": "TRANSIT"}]}]
                  }]
                }
                """);

        RouteStep step = routePlanner.findRoutes("Start", "End", TravelMode.TRANSIT)
                .get(0).steps().get(0);

        assertEquals("TRANSIT", step.travelMode());
        assertNull(step.transitDetails());
    }

    @Test
    void failsClearlyWhenApiKeyIsNotConfigured() {
        GoogleRoutesRoutePlanner routePlanner =
                new GoogleRoutesRoutePlanner(RestClient.builder(), "", ENDPOINT);

        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> routePlanner.findRoutes("Central Station", "City Museum", TravelMode.WALK));

        assertEquals("Google Routes API key is not configured", exception.getMessage());
    }

    private static GoogleRoutesRoutePlanner routePlannerFor(String responseBody) {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer.bindTo(restClientBuilder)
                .build()
                .expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-FieldMask", FIELD_MASK))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
        return new GoogleRoutesRoutePlanner(restClientBuilder, "test-api-key", ENDPOINT);
    }
}
