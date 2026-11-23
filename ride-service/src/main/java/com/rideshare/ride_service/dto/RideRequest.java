package com.rideshare.ride_service.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideRequest {

    @NotBlank(message = "Rider id is required")
    private String riderId;

    @NotNull(message = "Pick up latitude is required")
    private double pickUpLatitude;

    @NotNull(message = "Pick up longitude is required")
    private double pickupLongitude;

    @NotNull(message = "Pickup Address is required")
    private String pickupAddress;

    @NotNull(message = "drop latitude is required")
    private double dropLatitude;

    @NotNull(message = "drop longitude is required")
    private double dropLongitude;

    @NotNull(message = "drop address is required")
    private String dropAddress;

}
