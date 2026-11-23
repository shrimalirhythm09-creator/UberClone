package com.rideshare.matching_service.service;

import com.rideshare.matching_service.client.LocationServiceClient;
import com.rideshare.matching_service.dto.NearByDriverResponse;
import com.rideshare.matching_service.event.RideMatchEvent;
import com.rideshare.matching_service.event.RideRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final LocationServiceClient locationServiceClient;
    private final KafkaTemplate<String, RideMatchEvent>kafkaTemplate;

    private static  final String RIDE_MATCHED_TOPIC  = "ride.matched";
    private static final  double DEFAULT_SEARCH_RADIUS_KM = 5.0;

    public void matchDriverForRide(RideRequestEvent event){
        List<NearByDriverResponse>nearByDrivers = locationServiceClient.getNearByDrivers(
                event.getPickupLatitude(),
                event.getPickupLongitude(),
                DEFAULT_SEARCH_RADIUS_KM
        );

        if (nearByDrivers.isEmpty()){
            log.warn("No Driver found near ride");
            return;
        }

        Optional<NearByDriverResponse> bestDriver = findBestDriver(nearByDrivers);

        if (bestDriver.isEmpty()){
            log.warn("Could not find suitable  driver for ride");
            return;
        }

        NearByDriverResponse assignDriver = bestDriver.get();

        RideMatchEvent matchEvent = new RideMatchEvent(
                event.getRideId(),
                event.getRiderId(),
                event.getDriverId(),
                assignDriver.getLatitude(),
                assignDriver.getLongitude(),
                assignDriver.getDistanceInKm()
        );

        kafkaTemplate.send(RIDE_MATCHED_TOPIC,event.getRideId(),matchEvent);
        log.info("RideMatchedEvent is published");
    }
    private Optional<NearByDriverResponse>findBestDriver(
            List<NearByDriverResponse> drivers){

        double distanceWeight = 0.7;
        double ratingWeight = 0.3;

        return drivers.stream()
                .max(Comparator.comparingDouble(driver->{

                    double  distanceScore = 1.0/(driver.getDistanceInKm()+0.1);
                    double simulatedRating = 4.0 + Math.random();

                    return (distanceScore*distanceWeight)+
                            (simulatedRating * ratingWeight);
                }));
    }
}
