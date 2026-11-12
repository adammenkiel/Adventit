package org.adventit.adapter.out.routes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.adventit.domain.trip.TravelMode;
import org.adventit.domain.trip.TripRoute;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleRoutesRoutePlannerTest {
    private static final String ENDPOINT = "https://routes.googleapis.com/directions/v2:computeRoutes";

    @Test
    void sendsGoogleRoutesRequestAndMapsRouteResponse() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "test-api-key"))
                .andExpect(header(
                        "X-Goog-FieldMask",
                        "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline"))
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
                            "polyline": {"encodedPolyline": "abc"}
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));
        GoogleRoutesRoutePlanner routePlanner =
                new GoogleRoutesRoutePlanner(restClientBuilder, "test-api-key", ENDPOINT);

        List<TripRoute> routes = routePlanner.findRoutes(
                "Central Station",
                "City Museum",
                TravelMode.TRANSIT);

        assertEquals(List.of(new TripRoute(124, 1200, "abc")), routes);
        server.verify();
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
}
