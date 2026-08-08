package com.smartbus.backend.service;

import com.smartbus.backend.dto.BoardingRequestResponse;
import com.smartbus.backend.dto.CreateBoardingRequest;
import com.smartbus.backend.dto.PassengerTripTrackingResponse;
import com.smartbus.backend.dto.TripResponse;
import java.util.List;

public interface BoardingRequestService {

    List<TripResponse> findActiveTrips(Long routeId, Long boardingStopId, Long destinationStopId);

    BoardingRequestResponse create(CreateBoardingRequest request);

    List<BoardingRequestResponse> listMine();

    List<BoardingRequestResponse> listByTrip(Long tripId);

    BoardingRequestResponse confirmBoarding(Long id);

    BoardingRequestResponse cancel(Long id);

    PassengerTripTrackingResponse track(Long id);
}
