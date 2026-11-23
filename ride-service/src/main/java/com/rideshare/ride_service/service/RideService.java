package com.rideshare.ride_service.service;


import com.rideshare.ride_service.dto.RideRequest;
import com.rideshare.ride_service.dto.RideResponse;
import com.rideshare.ride_service.event.RideRequestEvent;
import com.rideshare.ride_service.model.Ride;
import com.rideshare.ride_service.model.RideStatus;
import com.rideshare.ride_service.repositary.RideRepositary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RideService {

    private final RideRepositary rideRepositary;
    private final KafkaTemplate<String,RideRequestEvent>kafkaTemplate;
    private static final String RIDE_REQUESTED_TOPIC = "ride.requested";

    public RideResponse requestRide(RideRequest request) {
        log.info("New Ride request from rider :{} ",request.getRiderId());

        Ride ride  = new Ride();
        ride.setPickupLatitude(request.getPickUpLatitude());
        ride.setPickupLongitude(request.getPickupLongitude());
        ride.setPickupAddress(request.getPickupAddress());

        // Drop details
        ride.setDropLatitude(request.getDropLatitude());
        ride.setDropLongitude(request.getDropLongitude());
        ride.setDropAddress(request.getDropAddress());

        // Initial ride status
        ride.setStatus(RideStatus.REQUESTED);
        ride.setEstimatedFair(calculateEstimatedFair(request));

        Ride savedRide = rideRepositary.save(ride);

        RideRequestEvent event = new RideRequestEvent(
        savedRide.getId(),
         savedRide.getRiderId(),
           savedRide.getPickupLongitude(),
                savedRide.getPickupLatitude(),
                savedRide.getPickupAddress(),
                savedRide.getDropLatitude(),
                savedRide.getDropLongitude(),
                savedRide.getDropAddress()
        );
      kafkaTemplate.send(RIDE_REQUESTED_TOPIC,savedRide.getId(),event);
      log.info("RideRequestEvent publish to kakfa for ride:{}",savedRide.getId());
      
      savedRide.setStatus(RideStatus.MATCHING);
      
      rideRepositary.save(savedRide);
      
      return mapToResponse(savedRide);

    }

    public void updateRideWithDriver(String rideId,String driverId){
        Ride ride = rideRepositary.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));


        ride.setDriverId(driverId);
        ride.setStatus(RideStatus.ACCEPTED);
        rideRepositary.save(ride);
    }

    private RideResponse mapToResponse(Ride ride) {

        RideResponse response = new RideResponse();

        response.setId(ride.getId());
        response.setRiderId(ride.getRiderId());
        response.setDriverId(ride.getDriverId());

        // Pickup details
        response.setPickupLatitude(ride.getPickupLatitude());
        response.setPickupLongitude(ride.getPickupLongitude());
        response.setPickupAddress(ride.getPickupAddress());

        // Drop details
        response.setDropLatitude(ride.getDropLatitude());
        response.setDropLongitude(ride.getDropLongitude());
        response.setDropAddress(ride.getDropAddress());

        // Ride information
        response.setStatus(ride.getStatus());
        response.setEstimatedFair(ride.getEstimatedFair());
        response.setActualFair(ride.getActualFair());

        // Timestamps
        response.setCreatedAt(ride.getCreatedAt());
        response.setUpdatedAt(ride.getUpdatedAt());
        response.setStartedAt(ride.getStartedAt());
        response.setCompleteAt(ride.getCompleteAt());

        return response;
    }

    private double calculateEstimatedFair(RideRequest request) {

        double lat1 = Math.toRadians(request.getPickUpLatitude());
        double lat2 = Math.toRadians(request.getDropLatitude());

        double long1 = Math.toRadians(request.getPickupLongitude());
        double long2 = Math.toRadians(request.getDropLongitude());


        double dLat = lat2 - lat1;
        double dLong = long2 - long1;

        double a = Math.pow(Math.sin(dLat / 2),2) +
                Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dLong/2),2);

        double c = 2 * Math.asin(Math.sqrt(a));
        double distanceKm = 6371 * c;

        Double fair = 50  + (distanceKm*12);

        return Math.round(fair*100.0)/100.0;

    }

    public RideResponse getRideById(String rideId) {
        Ride ride = rideRepositary.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        return mapToResponse(ride);
    }

    public List<RideResponse> getRidesByRider(String riderId) {

        return rideRepositary.findByRiderOrderByCreatedAtDesc(riderId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RideResponse startRide(String rideId) {
        Ride ride = rideRepositary.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (ride.getStatus() != RideStatus.ACCEPTED){
            throw new RuntimeException("Ride cannot be started, Current status : "+ride.getStatus());
        }

        ride.setStatus(RideStatus.RIDE_STARTED);
        ride.setCreatedAt(LocalDateTime.now());
        rideRepositary.save(ride);

        return mapToResponse(ride);
    }

    public RideResponse completeRide(String rideId) {
        Ride ride = rideRepositary.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (ride.getStatus() != RideStatus.RIDE_STARTED){
            throw new RuntimeException("Ride cannot be completed, Current status : "+ride.getStatus());
        }
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompleteAt(LocalDateTime.now());
        ride.setActualFair(ride.getEstimatedFair());
        rideRepositary.save(ride);

        return mapToResponse(ride);

    }

    public RideResponse cancelRide(String rideId) {
        Ride ride = rideRepositary.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        ride.setStatus(RideStatus.CANCELLED);
        rideRepositary.save(ride);

        return mapToResponse(ride);

    }
}
