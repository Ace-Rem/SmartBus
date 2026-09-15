package com.smartbus.backend.service;

import com.smartbus.backend.dto.FastBoardingSignalRequest;
import com.smartbus.backend.dto.FastBoardingSignalAcceptRequest;
import com.smartbus.backend.dto.FastBoardingAcceptanceResponse;
import com.smartbus.backend.dto.FastBoardingSignalCancelRequest;
import com.smartbus.backend.dto.FastBoardingSignalResponse;
import java.util.List;

public interface FastBoardingSignalService {

    FastBoardingSignalResponse publish(FastBoardingSignalRequest request);

    List<FastBoardingSignalResponse> poll(Long routeId, Long afterId);

    FastBoardingAcceptanceResponse accept(Long signalId, FastBoardingSignalAcceptRequest request);

    FastBoardingSignalResponse cancel(FastBoardingSignalCancelRequest request);

    List<FastBoardingAcceptanceResponse> pollAccepted(Long afterId);

    List<FastBoardingSignalResponse> pollCancelled(Long routeId, Long afterId);
}
