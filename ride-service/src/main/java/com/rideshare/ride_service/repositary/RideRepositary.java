package com.rideshare.ride_service.repositary;

import com.rideshare.ride_service.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRepositary extends JpaRepository<Ride,String> {

  List<Ride>findByRiderOrderByCreatedAtDesc(String  riderId);

}
