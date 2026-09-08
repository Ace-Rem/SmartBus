package com.smartbus.backend.service;

import com.smartbus.backend.dto.FastBoardingSignalRequest;
import com.smartbus.backend.dto.FastBoardingSignalResponse;
import java.util.List;

public interface FastBoardingSignalService {

    FastBoardingSignalResponse publish(FastBoardingSignalRequest request);

    List<FastBoardingSignalResponse> poll(Long routeId, Long afterId);
}
