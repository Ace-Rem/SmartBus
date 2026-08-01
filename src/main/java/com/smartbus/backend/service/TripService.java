package com.smartbus.backend.service;

import com.smartbus.backend.dto.CreateTripRequest;
import com.smartbus.backend.dto.StopResponse;
import com.smartbus.backend.dto.TripResponse;
import com.smartbus.backend.dto.UpdateLocationRequest;
import com.smartbus.backend.dto.UpdateLocationResponse;
import java.util.List;

public interface TripService {

    TripResponse startTrip(CreateTripRequest request);

    TripResponse endCurrentTrip();

    TripResponse getCurrentTrip();

    List<TripResponse> getHistory();

    TripResponse getById(Long tripId);

    UpdateLocationResponse updateCurrentLocation(UpdateLocationRequest request);

    StopResponse getCurrentStop();

    StopResponse getNextStop();
}
