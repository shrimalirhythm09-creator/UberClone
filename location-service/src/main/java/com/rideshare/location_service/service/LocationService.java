package com.rideshare.location_service.service;

import com.rideshare.location_service.dto.DriverLocationRequest;
import com.rideshare.location_service.dto.NearByDriverResponse;
import org.springframework.data.redis.connection.RedisGeoCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {

    private final RedisTemplate<String ,String>redisTemplate;

    private static  final String DRIVERS_GEO_KEY = "drivers:locations";



    public void updateDriverLocation(DriverLocationRequest request){
     log.info("Updating Location for drivers :{}",request.getDriverId());

        Point driverPoint = new Point(
                request.getLongitude(),
                request.getLatitude());
        redisTemplate.opsForGeo().add(DRIVERS_GEO_KEY,driverPoint,request.getDriverId());

        log.info("Location updated for drivers : {}",request.getDriverId());
    }

    public List<NearByDriverResponse> findNearbyDrivers(
            double latitude, double longitude, double radius){

        log.info("Finding drivers near lat : {} long : {} withing {}km",
                latitude,longitude,radius);

        Circle searchArea = new Circle(
                new Point(longitude,latitude),
                new Distance(radius, Metrics.KILOMETERS)
        );

       GeoResults<RedisGeoCommands.GeoLocation<String>> results =
               redisTemplate.opsForGeo().radius(
                       DRIVERS_GEO_KEY,
                       searchArea,
                       RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                               .includeCoordinates()
                               .includeDistance()
                               .sortAscending()
                               .limit(10)
               );

       List<NearByDriverResponse> nearByDriverResponses = new ArrayList<>();
       if (results != null){
           results.getContent().forEach(result ->{
               RedisGeoCommands.GeoLocation<String> location = result.getContent();
               nearByDriverResponses.add(new NearByDriverResponse(
                       location.getName(),
                       location.getPoint().getY(),
                       location.getPoint().getX(),
                       result.getDistance().getValue()
               ));
           });
       }

       log.info("Found {} Drivers nearby",nearByDriverResponses.size());
       return nearByDriverResponses;
    }

    public void removeDriver(String driverId) {
        log.info("Removing driver : {}",driverId);
        redisTemplate.opsForGeo().remove(DRIVERS_GEO_KEY,driverId);
    }
}
