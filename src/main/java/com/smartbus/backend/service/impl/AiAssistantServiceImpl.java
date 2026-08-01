package com.smartbus.backend.service.impl;

import com.smartbus.backend.ai.AiClient;
import com.smartbus.backend.ai.AiPromptBuilder;
import com.smartbus.backend.ai.AiRequest;
import com.smartbus.backend.ai.AiResponse;
import com.smartbus.backend.dto.AiAssistantRequest;
import com.smartbus.backend.dto.AiAssistantResponse;
import com.smartbus.backend.dto.AiSummaryRequest;
import com.smartbus.backend.entity.PassengerRecord;
import com.smartbus.backend.entity.Stop;
import com.smartbus.backend.entity.Trip;
import com.smartbus.backend.exception.ForbiddenException;
import com.smartbus.backend.exception.ResourceNotFoundException;
import com.smartbus.backend.repository.PassengerRecordRepository;
import com.smartbus.backend.repository.StopRepository;
import com.smartbus.backend.repository.TripRepository;
import com.smartbus.backend.security.SecurityUtils;
import com.smartbus.backend.service.AiAssistantService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private final TripRepository tripRepository;
    private final PassengerRecordRepository passengerRecordRepository;
    private final StopRepository stopRepository;
    private final AiPromptBuilder aiPromptBuilder;
    private final AiClient aiClient;

    public AiAssistantServiceImpl(
            TripRepository tripRepository,
            PassengerRecordRepository passengerRecordRepository,
            StopRepository stopRepository,
            AiPromptBuilder aiPromptBuilder,
            AiClient aiClient
    ) {
        this.tripRepository = tripRepository;
        this.passengerRecordRepository = passengerRecordRepository;
        this.stopRepository = stopRepository;
        this.aiPromptBuilder = aiPromptBuilder;
        this.aiClient = aiClient;
    }

    @Override
    @Transactional(readOnly = true)
    public AiAssistantResponse chat(AiAssistantRequest request) {
        Map<String, Object> context = buildTripContext(request.getTripId());
        String prompt = aiPromptBuilder.buildChatPrompt(context, request.getQuestion());
        return invoke(prompt);
    }

    @Override
    @Transactional(readOnly = true)
    public AiAssistantResponse summarize(AiSummaryRequest request) {
        Map<String, Object> context = buildTripContext(request.getTripId());
        String prompt = aiPromptBuilder.buildSummaryPrompt(context);
        return invoke(prompt);
    }

    private AiAssistantResponse invoke(String prompt) {
        AiRequest aiRequest = new AiRequest();
        aiRequest.setPrompt(prompt);
        AiResponse aiResponse = aiClient.complete(aiRequest);
        AiAssistantResponse response = new AiAssistantResponse();
        response.setAnswer(aiResponse.getContent());
        return response;
    }

    /**
     * Backend aggregates trip facts. AI only narrates from this context — never computes business rules.
     */
    private Map<String, Object> buildTripContext(Long tripId) {
        Long driverId = SecurityUtils.requireCurrentDriverId();
        Trip trip = tripRepository.findByIdWithDetails(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new ForbiddenException("Trip does not belong to current driver");
        }

        List<PassengerRecord> records = passengerRecordRepository.findByTripIdOrderByRecordedAtAsc(tripId);
        int totalPassengers = records.stream()
                .mapToInt(record -> record.getPassengerCount() == null ? 0 : record.getPassengerCount())
                .sum();

        String passengerSummary = records.stream()
                .map(record -> {
                    String stopPart = record.getStop() == null
                            ? "stop=(none)"
                            : "stop=" + record.getStop().getName() + "(id=" + record.getStop().getId() + ")";
                    String notePart = record.getNote() == null || record.getNote().isBlank()
                            ? ""
                            : ", note=" + record.getNote();
                    return stopPart + ", count=" + record.getPassengerCount() + notePart;
                })
                .collect(Collectors.joining("; "));

        List<Stop> routeStops = stopRepository.findByRouteIdAndActiveTrueOrderByStopOrderAsc(
                trip.getRoute().getId()
        );

        Stop currentStop = trip.getCurrentStop();
        Stop nextStop = null;
        int remainingStops = 0;
        if (currentStop != null) {
            nextStop = stopRepository
                    .findFirstByRouteIdAndActiveTrueAndStopOrderGreaterThanOrderByStopOrderAsc(
                            trip.getRoute().getId(),
                            currentStop.getStopOrder()
                    )
                    .orElse(null);
            remainingStops = (int) routeStops.stream()
                    .filter(stop -> stop.getStopOrder() != null
                            && stop.getStopOrder() > currentStop.getStopOrder())
                    .count();
        } else {
            remainingStops = routeStops.size();
        }

        int passengersAlightingAtCurrent = 0;
        if (currentStop != null) {
            passengersAlightingAtCurrent = passengerRecordRepository
                    .sumPassengerCountByTripIdAndStopId(tripId, currentStop.getId());
        }

        int passengersAlightingAtNext = 0;
        if (nextStop != null) {
            passengersAlightingAtNext = passengerRecordRepository
                    .sumPassengerCountByTripIdAndStopId(tripId, nextStop.getId());
        }

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("tripId", trip.getId());
        context.put("status", trip.getStatus());
        context.put("routeName", trip.getRoute().getName());
        context.put("routeCode", trip.getRoute().getCode());
        context.put("startedAt", trip.getStartedAt());
        context.put("endedAt", trip.getEndedAt());
        context.put("currentStopName", currentStop == null ? null : currentStop.getName());
        context.put("currentStopOrder", currentStop == null ? null : currentStop.getStopOrder());
        context.put("nextStopName", nextStop == null ? null : nextStop.getName());
        context.put("nextStopOrder", nextStop == null ? null : nextStop.getStopOrder());
        context.put("remainingStopsCount", remainingStops);
        context.put("totalStopsOnRoute", routeStops.size());
        context.put("totalPassengers", totalPassengers);
        context.put("passengerGroupCount", records.size());
        context.put("passengersAlightingAtCurrentStop", passengersAlightingAtCurrent);
        context.put("passengersAlightingAtNextStop", passengersAlightingAtNext);
        context.put("currentLatitude", trip.getCurrentLatitude());
        context.put("currentLongitude", trip.getCurrentLongitude());
        context.put("passengerGroups", passengerSummary.isBlank() ? "(none)" : passengerSummary);
        return context;
    }
}
