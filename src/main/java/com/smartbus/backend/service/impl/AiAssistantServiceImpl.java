package com.smartbus.backend.service.impl;

import com.smartbus.backend.ai.AiClient;
import com.smartbus.backend.ai.AiPromptBuilder;
import com.smartbus.backend.ai.AiRequest;
import com.smartbus.backend.ai.AiResponse;
import com.smartbus.backend.ai.OpenAiClient;
import com.smartbus.backend.dto.AiAssistantRequest;
import com.smartbus.backend.dto.AiAssistantResponse;
import com.smartbus.backend.dto.AiSummaryRequest;
import com.smartbus.backend.entity.PassengerRecord;
import com.smartbus.backend.entity.Stop;
import com.smartbus.backend.entity.Trip;
import com.smartbus.backend.exception.ForbiddenException;
import com.smartbus.backend.exception.ResourceNotFoundException;
import com.smartbus.backend.repository.PassengerRecordRepository;
import com.smartbus.backend.repository.BoardingRequestRepository;
import com.smartbus.backend.repository.StopRepository;
import com.smartbus.backend.repository.TripRepository;
import com.smartbus.backend.security.SecurityUtils;
import com.smartbus.backend.service.AiAssistantService;
import com.smartbus.backend.util.GeoUtils;
import com.smartbus.backend.util.TripStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private final TripRepository tripRepository;
    private final BoardingRequestRepository boardingRequestRepository;
    private final PassengerRecordRepository passengerRecordRepository;
    private final StopRepository stopRepository;
    private final AiPromptBuilder aiPromptBuilder;
    private final AiClient aiClient;

    public AiAssistantServiceImpl(
            TripRepository tripRepository,
            BoardingRequestRepository boardingRequestRepository,
            PassengerRecordRepository passengerRecordRepository,
            StopRepository stopRepository,
            AiPromptBuilder aiPromptBuilder,
            AiClient aiClient
    ) {
        this.tripRepository = tripRepository;
        this.boardingRequestRepository = boardingRequestRepository;
        this.passengerRecordRepository = passengerRecordRepository;
        this.stopRepository = stopRepository;
        this.aiPromptBuilder = aiPromptBuilder;
        this.aiClient = aiClient;
    }

    @Override
    @Transactional(readOnly = true)
    public AiAssistantResponse chat(AiAssistantRequest request) {
        Map<String, Object> context = buildTripContext(request.getTripId(), request.getClientContext());
        String prompt = aiPromptBuilder.buildChatPrompt(context, request.getQuestion());
        return invoke(prompt, context, request.getQuestion());
    }

    @Override
    @Transactional(readOnly = true)
    public AiAssistantResponse summarize(AiSummaryRequest request) {
        Map<String, Object> context = buildTripContext(request.getTripId(), request.getClientContext());
        String prompt = aiPromptBuilder.buildSummaryPrompt(context);
        return invoke(prompt, context, "tom tat chuyen");
    }

    private void mergeClientContext(Map<String, Object> context, Map<String, Object> clientContext) {
        if (context == null || clientContext == null || clientContext.isEmpty()) {
            return;
        }
        clientContext.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                context.put("client." + key, value);
            }
        });
    }

    private AiAssistantResponse invoke(String prompt, Map<String, Object> context, String question) {
        AiRequest aiRequest = new AiRequest();
        aiRequest.setPrompt(prompt);
        AiResponse aiResponse = aiClient.complete(aiRequest);
        String answer = aiResponse == null ? null : aiResponse.getContent();
        // Old Render builds returned a fixed apology string instead of DB answers.
        // Treat that (and FALLBACK_MARKER) as failure → answer from trip context.
        if (answer == null
                || answer.isBlank()
                || OpenAiClient.FALLBACK_MARKER.equals(answer)
                || OpenAiClient.isUnavailableApology(answer)) {
            answer = aiPromptBuilder.buildDataDrivenAnswer(context, question);
        }
        AiAssistantResponse response = new AiAssistantResponse();
        response.setAnswer(answer);
        return response;
    }

    /**
     * Backend aggregates trip facts from DB. AI only narrates from this context.
     */
    private Map<String, Object> buildTripContext(Long tripId, Map<String, Object> clientContext) {
        Trip trip = tripRepository.findByIdWithDetails(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));
        Map<String, Object> context = new LinkedHashMap<>();
        mergeClientContext(context, clientContext);
        authorizeTripContext(trip, context);

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

        List<Stop> routeStops = trip.getRoute() == null
                ? List.of()
                : stopRepository.findByRouteIdAndActiveTrueOrderByStopOrderAsc(trip.getRoute().getId());
        String stopsOnRoute = routeStops.stream()
                .map(stop -> (stop.getStopOrder() == null ? "?" : stop.getStopOrder())
                        + ". " + stop.getName())
                .collect(Collectors.joining(" | "));

        // Align with GPS UI: current = nearest to vehicle, next = following stop on route.
        Stop resolvedCurrent = trip.getCurrentStop();
        Double nearestDistanceMeters = null;
        if (trip.getCurrentLatitude() != null && trip.getCurrentLongitude() != null) {
            Stop nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (Stop stop : routeStops) {
                if (stop.getLatitude() == null || stop.getLongitude() == null) {
                    continue;
                }
                double distance = GeoUtils.distanceMeters(
                        trip.getCurrentLatitude(),
                        trip.getCurrentLongitude(),
                        stop.getLatitude(),
                        stop.getLongitude()
                );
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = stop;
                }
            }
            if (nearest != null) {
                resolvedCurrent = nearest;
                nearestDistanceMeters = nearestDistance;
            }
        }
        final Stop currentStop = resolvedCurrent;

        Stop nextStop = null;
        int remainingStops = 0;
        if (currentStop != null && currentStop.getStopOrder() != null && trip.getRoute() != null) {
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

        context.put("tripId", trip.getId());
        context.put("status", trip.getStatus());
        context.put("driverName", trip.getDriver() == null ? null : trip.getDriver().getFullName());
        context.put("driverPhone", trip.getDriver() == null ? null : trip.getDriver().getPhoneNumber());
        context.put("routeName", trip.getRoute() == null ? null : trip.getRoute().getName());
        context.put("routeCode", trip.getRoute() == null ? null : trip.getRoute().getCode());
        context.put("routeDescription", trip.getRoute() == null ? null : trip.getRoute().getDescription());
        context.put("startedAt", trip.getStartedAt());
        context.put("endedAt", trip.getEndedAt());
        context.put("currentStopName", currentStop == null ? null : currentStop.getName());
        context.put("currentStopOrder", currentStop == null ? null : currentStop.getStopOrder());
        context.put("nextStopName", nextStop == null ? null : nextStop.getName());
        context.put("nextStopOrder", nextStop == null ? null : nextStop.getStopOrder());
        context.put("remainingStopsCount", remainingStops);
        context.put("totalStopsOnRoute", routeStops.size());
        context.put("stopsOnRoute", stopsOnRoute.isBlank() ? "(none)" : stopsOnRoute);
        context.put("totalPassengers", totalPassengers);
        context.put("passengerGroupCount", records.size());
        context.put("passengersAlightingAtCurrentStop", passengersAlightingAtCurrent);
        context.put("passengersAlightingAtNextStop", passengersAlightingAtNext);
        context.put("currentLatitude", trip.getCurrentLatitude());
        context.put("currentLongitude", trip.getCurrentLongitude());
        context.put("nearestStopDistanceMeters", nearestDistanceMeters);
        context.put("passengerGroups", passengerSummary.isBlank() ? "(none)" : passengerSummary);
        return context;
    }

    private void authorizeTripContext(Trip trip, Map<String, Object> context) {
        Long driverId = SecurityUtils.currentDriverIdOrNull();
        if (driverId != null) {
            if (trip.getDriver() == null || !trip.getDriver().getId().equals(driverId)) {
                throw new ForbiddenException("Trip does not belong to current driver");
            }
            return;
        }
        Long passengerId = SecurityUtils.currentPassengerIdOrNull();
        if (passengerId != null
                && trip.getId() != null
                && boardingRequestRepository.existsByPassengerIdAndTripId(passengerId, trip.getId())) {
            return;
        }
        if (passengerId != null
                && trip.getId() != null
                && TripStatus.IN_PROGRESS.equals(trip.getStatus())
                && trip.getId().equals(clientSelectedTripId(context))) {
            return;
        }
        throw new ForbiddenException("Trip does not belong to current account");
    }

    private Long clientSelectedTripId(Map<String, Object> context) {
        if (context == null) {
            return null;
        }
        Object raw = context.get("client.selectedTripId");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
