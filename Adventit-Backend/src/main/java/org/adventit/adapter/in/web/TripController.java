package org.adventit.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.adventit.application.port.in.PlanTripUseCase;
import org.adventit.domain.trip.TripPlan;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TripController {
    private final PlanTripUseCase planTrip;

    public TripController(PlanTripUseCase planTrip) {
        this.planTrip = planTrip;
    }

    @PostMapping("/app/trip")
    public TripPlan planTrip(@Valid @RequestBody PlanTripRequest request) {
        return planTrip.planTrip(request.startPlace(), request.endPlace());
    }

    public record PlanTripRequest(
            @NotBlank @Size(max = 500) String startPlace,
            @NotBlank @Size(max = 500) String endPlace) {
    }
}
