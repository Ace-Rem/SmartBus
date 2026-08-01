package com.smartbus.backend.service;

import com.smartbus.backend.dto.CreatePassengerRecordRequest;
import com.smartbus.backend.dto.PassengerRecordResponse;
import java.util.List;

public interface PassengerRecordService {

    PassengerRecordResponse create(CreatePassengerRecordRequest request);

    List<PassengerRecordResponse> listByTrip(Long tripId);
}
