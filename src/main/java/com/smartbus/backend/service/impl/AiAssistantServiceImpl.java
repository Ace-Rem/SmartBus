package com.smartbus.backend.service.impl;

import com.smartbus.backend.ai.AiClient;
import com.smartbus.backend.ai.AiRequest;
import com.smartbus.backend.ai.AiResponse;
import com.smartbus.backend.ai.OpenAiClient;
import com.smartbus.backend.ai.SmartBusAiTools;
import com.smartbus.backend.dto.AiAssistantRequest;
import com.smartbus.backend.dto.AiAssistantResponse;
import com.smartbus.backend.dto.AiConversationMessage;
import com.smartbus.backend.dto.AiSummaryRequest;
import com.smartbus.backend.entity.PassengerRecord;
import com.smartbus.backend.entity.Route;
import com.smartbus.backend.entity.Stop;
import com.smartbus.backend.entity.Trip;
import com.smartbus.backend.exception.ForbiddenException;
import com.smartbus.backend.exception.ResourceNotFoundException;
import com.smartbus.backend.repository.PassengerRecordRepository;
import com.smartbus.backend.repository.BoardingRequestRepository;
import com.smartbus.backend.repository.RouteRepository;
import com.smartbus.backend.repository.StopRepository;
import com.smartbus.backend.repository.TripRepository;
import com.smartbus.backend.security.SecurityUtils;
import com.smartbus.backend.service.AiAssistantService;
import com.smartbus.backend.service.AiContextService;
import com.smartbus.backend.util.GeoUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private final TripRepository tripRepository;
    private final BoardingRequestRepository boardingRequestRepository;
    private final PassengerRecordRepository passengerRecordRepository;
    private final StopRepository stopRepository;
    private final AiClient aiClient;
    private final AiContextService aiContextService;
    private final RouteRepository routeRepository;
    private final SmartBusAiTools aiTools;
    private final ObjectMapper objectMapper;

    public AiAssistantServiceImpl(
            TripRepository tripRepository,
            BoardingRequestRepository boardingRequestRepository,
            PassengerRecordRepository passengerRecordRepository,
            StopRepository stopRepository,
            AiClient aiClient,
            AiContextService aiContextService,
            RouteRepository routeRepository,
            SmartBusAiTools aiTools,
            ObjectMapper objectMapper
    ) {
        this.tripRepository = tripRepository;
        this.boardingRequestRepository = boardingRequestRepository;
        this.passengerRecordRepository = passengerRecordRepository;
        this.stopRepository = stopRepository;
        this.aiClient = aiClient;
        this.aiContextService = aiContextService;
        this.routeRepository = routeRepository;
        this.aiTools = aiTools;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AiAssistantResponse chat(AiAssistantRequest request) {
        Map<String, Object> context = buildTripContext(
                request.getTripId(), request.getRouteId(), request.getClientContext());
        return invoke(context, request.getQuestion(), request.getConversationHistory());
    }

    @Override
    @Transactional(readOnly = true)
    public AiAssistantResponse summarize(AiSummaryRequest request) {
        Map<String, Object> context = buildTripContext(
                request.getTripId(), request.getRouteId(), request.getClientContext());
        return invoke(context, "Tóm tắt hành trình", request.getConversationHistory());
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

    private AiAssistantResponse invoke(Map<String, Object> context, String question,
                                       List<AiConversationMessage> history) {
        AiRequest aiRequest = new AiRequest();
        aiRequest.addMessage("system", "Bạn là trợ lý SmartBus. Hiểu câu hỏi tự nhiên bằng ngôn ngữ người dùng. "
                + "Chỉ dùng dữ liệu từ công cụ hoặc CONTEXT được cấp; không bịa, không lộ token hay thông tin ngoài quyền. "
                + "Với dữ liệu GPS, luôn nói rõ thời điểm và nguồn nếu có. Nếu thiếu dữ liệu, nói rõ thiếu gì.");
        appendHistory(aiRequest, history);
        aiRequest.addMessage("user", "CONTEXT hiện tại (chỉ là gợi ý cục bộ, có thể cũ): "
                + compactContext(context) + "\n\nCâu hỏi: " + question);
        aiRequest.setTools(aiTools.definitions());

        String answer = null;
        for (int round = 0; round < 6; round++) {
            AiResponse aiResponse = aiClient.complete(aiRequest);
            if (aiResponse == null) break;
            if (aiResponse.hasToolCalls()) {
                Map<String, Object> assistant = new LinkedHashMap<>();
                assistant.put("role", "assistant");
                assistant.put("content", aiResponse.getContent());
                assistant.put("tool_calls", aiResponse.getToolCalls());
                aiRequest.getMessages().add(assistant);
                for (Map<String, Object> toolCall : aiResponse.getToolCalls()) {
                    Map<String, Object> function = asMap(toolCall.get("function"));
                    String name = String.valueOf(function.getOrDefault("name", ""));
                    Object rawArguments = function.containsKey("arguments")
                            ? function.get("arguments") : function.get("args");
                    Map<String, Object> arguments = arguments(rawArguments);
                    Map<String, Object> result = aiTools.execute(name, arguments, context);
                    Map<String, Object> toolMessage = new LinkedHashMap<>();
                    toolMessage.put("role", "tool");
                    toolMessage.put("tool_call_id", toolCall.get("id"));
                    toolMessage.put("name", name);
                    toolMessage.put("content", json(result));
                    aiRequest.getMessages().add(toolMessage);
                }
                continue;
            }
            answer = aiResponse.getContent();
            break;
        }
        if (OpenAiClient.isUnavailableApology(answer)) {
            answer = "Dịch vụ AI bên ngoài hiện chưa khả dụng. Dữ liệu SmartBus cục bộ vẫn được giữ nguyên; "
                    + "hãy thử lại khi có kết nối và cấu hình provider.";
        }
        AiAssistantResponse response = new AiAssistantResponse();
        response.setAnswer(answer);
        return response;
    }

    private void appendHistory(AiRequest request, List<AiConversationMessage> history) {
        if (history == null) return;
        int start = Math.max(0, history.size() - 12);
        for (int i = start; i < history.size(); i++) {
            AiConversationMessage message = history.get(i);
            if (message == null || message.getContent() == null || message.getContent().isBlank()) continue;
            String role = "assistant".equals(message.getRole()) ? "assistant" : "user";
            request.addMessage(role, message.getContent().substring(0, Math.min(2000, message.getContent().length())));
        }
    }

    private String compactContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) return "(trống)";
        Map<String, Object> compact = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String key = entry.getKey();
            if ("stops".equals(key) || "passengerGroups".equals(key)
                    || key.endsWith(".stops") || key.endsWith(".passengerGroups")) continue;
            if (entry.getValue() != null && count++ < 80) compact.put(entry.getKey(), entry.getValue());
        }
        return json(compact);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> ? objectMapper.convertValue(value, Map.class) : Map.of();
    }

    private Map<String, Object> arguments(Object value) {
        if (value instanceof Map<?, ?>) return asMap(value);
        if (value == null) return Map.of();
        try { return objectMapper.readValue(String.valueOf(value), Map.class); }
        catch (Exception ignored) { return Map.of(); }
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { return "{\"error\":\"unserializable tool result\"}"; }
    }

    /**
     * Backend aggregates trip facts from DB. AI only narrates from this context.
     */
    private Map<String, Object> buildTripContext(
            Long tripId,
            Long routeId,
            Map<String, Object> clientContext
    ) {
        Map<String, Object> context = new LinkedHashMap<>();
        mergeClientContext(context, aiContextService.loadForCurrentAccount(tripId, routeId));
        mergeClientContext(context, clientContext);
        Trip trip = tripId == null ? null : tripRepository.findByIdWithDetails(tripId).orElse(null);
        if (trip == null) {
            if (routeId != null) {
                Route route = routeRepository.findById(routeId).orElse(null);
                if (route != null) addRouteContext(context, route);
            }
            if (context.isEmpty()) {
                throw new ResourceNotFoundException("Trip not found: " + tripId);
            }
            promoteClientValue(context, "status", "tripStatus", "currentTripStatus");
            promoteClientValue(context, "routeName", "currentRouteName", "selectedRouteName");
            promoteClientValue(context, "routeCode", "currentRouteCode", "selectedRouteCode");
            promoteClientValue(context, "currentStopName", "currentStopName");
            promoteClientValue(context, "currentStopOrder", "currentStopOrder");
            promoteClientValue(context, "nextStopName", "nextStopName");
            promoteClientValue(context, "nextStopOrder", "nextStopOrder");
            promoteClientValue(context, "totalPassengers", "totalPassengers", "passengerTotal", "passengerTotalOnBoard");
            promoteClientValue(context, "passengerGroupCount", "passengerGroupCount");
            promoteClientValue(context, "passengerGroups", "passengerGroups");
            promoteClientValue(context, "stopsOnRoute", "stopsOnRoute");
            promoteClientValue(context, "remainingStopsCount", "remainingStopsCount");
            promoteClientValue(context, "totalStopsOnRoute", "totalStopsOnRoute");
            context.putIfAbsent("tripId", tripId);
            context.putIfAbsent("status", "LOCAL_CONTEXT_ONLY");
            return context;
        }
        authorizeTripContext(trip, context);
        Map<String, Object> driverContext = aiContextService.loadLatestDriverContext(
                trip.getId(),
                trip.getRoute() == null ? routeId : trip.getRoute().getId()
        );
        mergeNamedContext(context, "driver.", driverContext);

        List<PassengerRecord> records = passengerRecordRepository.findByTripIdOrderByRecordedAtAsc(tripId);
        int totalPassengers = records.stream()
                .mapToInt(record -> record.getPassengerCount() == null ? 0 : record.getPassengerCount())
                .sum();
        Integer liveDriverPassengerTotal = integerValue(driverContext, "passengerTotalOnBoard");
        if (liveDriverPassengerTotal == null) {
            liveDriverPassengerTotal = integerValue(driverContext, "passengerTotal");
        }
        if (liveDriverPassengerTotal != null) {
            // The driver snapshot includes local Room records that may still
            // be waiting for a slow POST. It is the freshest live counter;
            // DB records remain the fallback for older trips/sessions.
            totalPassengers = Math.max(0, liveDriverPassengerTotal);
        }

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
        if (passengerSummary.isBlank() && driverContext.get("passengerGroups") != null) {
            passengerSummary = String.valueOf(driverContext.get("passengerGroups"));
        }

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
        context.put("currentStopId", currentStop == null ? null : currentStop.getId());
        context.put("currentStopOrder", currentStop == null ? null : currentStop.getStopOrder());
        context.put("nextStopName", nextStop == null ? null : nextStop.getName());
        context.put("nextStopId", nextStop == null ? null : nextStop.getId());
        context.put("nextStopOrder", nextStop == null ? null : nextStop.getStopOrder());
        context.put("remainingStopsCount", remainingStops);
        context.put("totalStopsOnRoute", routeStops.size());
        context.put("stopsOnRoute", stopsOnRoute.isBlank() ? "(none)" : stopsOnRoute);
        context.put("totalPassengers", totalPassengers);
        Integer liveDriverGroupCount = integerValue(driverContext, "passengerGroupCount");
        context.put("passengerGroupCount", liveDriverGroupCount == null
                ? records.size() : Math.max(0, liveDriverGroupCount));
        context.put("passengersAlightingAtCurrentStop", passengersAlightingAtCurrent);
        context.put("passengersAlightingAtNextStop", passengersAlightingAtNext);
        context.put("currentLatitude", trip.getCurrentLatitude());
        context.put("currentLongitude", trip.getCurrentLongitude());
        context.put("nearestStopDistanceMeters", nearestDistanceMeters);
        context.put("passengerGroups", passengerSummary.isBlank() ? "(none)" : passengerSummary);
        // The driver app may have a newer local GPS snapshot than the last
        // backend trip-location write. Use it for vehicle facts, while keeping
        // passenger GPS under client.* and never confusing the two.
        preferNamedValue(context, "currentStopId", driverContext, "currentStopId");
        preferNamedValue(context, "currentStopName", driverContext, "currentStopName");
        preferNamedValue(context, "currentStopOrder", driverContext, "currentStopOrder");
        preferNamedValue(context, "nextStopId", driverContext, "nextStopId");
        preferNamedValue(context, "nextStopName", driverContext, "nextStopName");
        preferNamedValue(context, "nextStopOrder", driverContext, "nextStopOrder");
        preferNamedValue(context, "currentLatitude", driverContext, "currentLatitude");
        preferNamedValue(context, "currentLongitude", driverContext, "currentLongitude");
        putVehicleFact(context, driverContext, "vehicleLatitude", "currentLatitude");
        putVehicleFact(context, driverContext, "vehicleLongitude", "currentLongitude");
        putVehicleFact(context, driverContext, "vehicleCurrentStopId", "currentStopId");
        putVehicleFact(context, driverContext, "vehicleCurrentStopName", "currentStopName");
        putVehicleFact(context, driverContext, "vehicleCurrentStopOrder", "currentStopOrder");
        putVehicleFact(context, driverContext, "vehicleNextStopId", "nextStopId");
        putVehicleFact(context, driverContext, "vehicleNextStopName", "nextStopName");
        putVehicleFact(context, driverContext, "vehicleNextStopOrder", "nextStopOrder");
        return context;
    }

    private Integer integerValue(Map<String, Object> context, String key) {
        if (context == null || context.get(key) == null) return null;
        Object raw = context.get(key);
        if (raw instanceof Number number) return number.intValue();
        try {
            return Integer.valueOf(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void addRouteContext(Map<String, Object> context, Route route) {
        List<Stop> routeStops = stopRepository.findByRouteIdAndActiveTrueOrderByStopOrderAsc(route.getId());
        context.putIfAbsent("routeId", route.getId());
        context.putIfAbsent("routeCode", route.getCode());
        context.putIfAbsent("routeName", route.getName());
        context.putIfAbsent("routeDescription", route.getDescription());
        context.putIfAbsent("status", "ROUTE_SELECTED");
        context.putIfAbsent("totalStopsOnRoute", routeStops.size());
        context.putIfAbsent("stopsOnRoute", routeStops.stream()
                .map(stop -> (stop.getStopOrder() == null ? "?" : stop.getStopOrder())
                        + ". " + stop.getName())
                .collect(Collectors.joining(" | ")));
    }

    private void promoteClientValue(Map<String, Object> context, String target, String... candidates) {
        if (context.get(target) != null) return;
        for (String candidate : candidates) {
            Object value = context.get("client." + candidate);
            if (value != null) {
                context.put(target, value);
                return;
            }
        }
    }

    private void mergeNamedContext(Map<String, Object> target, String prefix,
                                   Map<String, Object> source) {
        if (source == null || source.isEmpty()) return;
        source.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                target.put(prefix + key, value);
            }
        });
    }

    private void preferNamedValue(Map<String, Object> context, String target,
                                  Map<String, Object> source, String key) {
        if (source == null) return;
        Object value = source.get(key);
        if (value != null) context.put(target, value);
    }

    private void putVehicleFact(Map<String, Object> context, Map<String, Object> driverContext,
                                String target, String source) {
        if (driverContext != null && driverContext.get(source) != null) {
            context.put(target, driverContext.get(source));
        }
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
        throw new ForbiddenException("Trip does not belong to current account");
    }

}
