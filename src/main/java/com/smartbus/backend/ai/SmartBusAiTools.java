package com.smartbus.backend.ai;

import com.smartbus.backend.entity.BoardingRequest;
import com.smartbus.backend.entity.Driver;
import com.smartbus.backend.entity.Passenger;
import com.smartbus.backend.entity.PassengerRecord;
import com.smartbus.backend.entity.Route;
import com.smartbus.backend.entity.Stop;
import com.smartbus.backend.entity.Trip;
import com.smartbus.backend.exception.ForbiddenException;
import com.smartbus.backend.repository.BoardingRequestRepository;
import com.smartbus.backend.repository.DriverRepository;
import com.smartbus.backend.repository.PassengerRecordRepository;
import com.smartbus.backend.repository.PassengerRepository;
import com.smartbus.backend.repository.RouteRepository;
import com.smartbus.backend.repository.StopRepository;
import com.smartbus.backend.repository.TripRepository;
import com.smartbus.backend.security.SecurityUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Read-only, permission checked data access used by model tool calls.
 * It deliberately returns small projections instead of serialising entities.
 */
@Component
public class SmartBusAiTools {
    private static final int MAX_ROWS = 25;
    private final TripRepository trips;
    private final RouteRepository routes;
    private final StopRepository stops;
    private final PassengerRecordRepository records;
    private final BoardingRequestRepository requests;
    private final PassengerRepository passengers;
    private final DriverRepository drivers;

    public SmartBusAiTools(TripRepository trips, RouteRepository routes, StopRepository stops,
                           PassengerRecordRepository records, BoardingRequestRepository requests,
                           PassengerRepository passengers, DriverRepository drivers) {
        this.trips = trips;
        this.routes = routes;
        this.stops = stops;
        this.records = records;
        this.requests = requests;
        this.passengers = passengers;
        this.drivers = drivers;
    }

    public List<Map<String, Object>> definitions() {
        return List.of(
                function("get_passenger_information", "Read the signed-in passenger profile only.",
                        object(List.of(), Map.of("passengerId", number()))),
                function("get_driver_information", "Read the current driver's profile, or the driver of an authorized passenger trip.",
                        object(List.of("tripId"), Map.of("tripId", number()))),
                function("get_route_information", "Read one route and its ordered stops.",
                        object(List.of("routeId"), Map.of("routeId", number()))),
                function("get_bus_information", "Read the authorized bus/trip status and current location.",
                        object(List.of("tripId"), Map.of("tripId", number()))),
                function("get_trip_information", "Read the current authorized trip and its live status.",
                        object(List.of("tripId"), Map.of("tripId", number()))),
                function("get_ticket_information", "Read the signed-in passenger's boarding requests, limited to the current trip.",
                        object(List.of("tripId", "limit"), Map.of("tripId", number(), "limit", integer()))),
                function("get_booking_information", "Read the signed-in passenger's booking/check-in state.",
                        object(List.of("tripId"), Map.of("tripId", number()))),
                function("get_checkin_information", "Read check-in records for an authorized trip.",
                        object(List.of("tripId", "limit"), Map.of("tripId", number(), "limit", integer()))),
                function("get_gps_location", "Read a fresh server GPS snapshot; never infer a location.",
                        object(List.of("tripId"), Map.of("tripId", number()))),
                function("get_vehicle_status", "Read vehicle/trip status and GPS freshness metadata.",
                        object(List.of("tripId"), Map.of("tripId", number()))),
                function("get_sync_status", "Read local context sync metadata supplied by the signed-in app.",
                        object(List.of("tripId"), Map.of("tripId", number()))),
                function("get_activity_history", "Read recent check-in activity for an authorized trip.",
                        object(List.of("tripId", "limit"), Map.of("tripId", number(), "limit", integer()))),
                function("search_smartbus_data", "Search only the signed-in user's authorized current SmartBus context.",
                        object(List.of("query", "limit"), Map.of("query", string(), "limit", integer()))));
    }

    public Map<String, Object> execute(String name, Map<String, Object> arguments,
                                       Map<String, Object> clientContext) {
        try {
            long tripId = id(arguments, "tripId", clientContext, "tripId", "selectedTripId", "currentTripId");
            return switch (name) {
                case "get_trip_information", "get_bus_information", "get_vehicle_status" -> trip(tripId, clientContext);
                case "get_gps_location" -> gps(tripId, clientContext);
                case "get_driver_information" -> driver(tripId, clientContext);
                case "get_route_information" -> route(id(arguments, "routeId", clientContext, "routeId", "selectedRouteId"));
                case "get_passenger_information" -> passenger(arguments);
                case "get_ticket_information", "get_booking_information" -> bookings(tripId, limit(arguments));
                case "get_checkin_information", "get_activity_history" -> checkins(tripId, limit(arguments));
                case "get_sync_status" -> syncStatus(clientContext);
                case "search_smartbus_data" -> search(arguments, clientContext, limit(arguments));
                default -> error("Unknown tool: " + name);
            };
        } catch (ForbiddenException exception) {
            return error("Access denied for this data");
        } catch (RuntimeException exception) {
            return error("Data is not available: " + exception.getClass().getSimpleName());
        }
    }

    private Map<String, Object> trip(long id, Map<String, Object> context) {
        Trip value = authorizedTrip(id, context);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tripId", value.getId()); result.put("status", value.getStatus());
        result.put("routeId", value.getRoute() == null ? null : value.getRoute().getId());
        result.put("routeName", value.getRoute() == null ? null : value.getRoute().getName());
        result.put("routeCode", value.getRoute() == null ? null : value.getRoute().getCode());
        result.put("startedAt", value.getStartedAt()); result.put("endedAt", value.getEndedAt());
        result.put("currentStop", stop(value.getCurrentStop()));
        result.put("latitude", value.getCurrentLatitude()); result.put("longitude", value.getCurrentLongitude());
        result.put("locationUpdatedAt", value.getUpdatedAt());
        result.put("source", "SERVER_TRIP");
        return result;
    }

    private Map<String, Object> gps(long id, Map<String, Object> context) {
        Map<String, Object> result = trip(id, context);
        result.put("available", result.get("latitude") != null && result.get("longitude") != null);
        result.put("warning", result.get("available").equals(Boolean.TRUE)
                ? "Coordinates are the latest server snapshot; verify freshness before calling them current."
                : "No server GPS snapshot is available.");
        return result;
    }

    private Map<String, Object> driver(long tripId, Map<String, Object> context) {
        Trip trip = authorizedTrip(tripId, context);
        Driver driver = trip.getDriver();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("driverId", driver == null ? null : driver.getId());
        result.put("fullName", driver == null ? null : driver.getFullName());
        if (SecurityUtils.currentDriverIdOrNull() != null) result.put("phoneNumber", driver == null ? null : driver.getPhoneNumber());
        result.put("privacy", "Passenger accounts receive the driver's name only.");
        return result;
    }

    private Map<String, Object> route(long id) {
        Route route = routes.findById(id).orElseThrow();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("routeId", route.getId()); result.put("code", route.getCode()); result.put("name", route.getName());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Stop stop : stops.findByRouteIdAndActiveTrueOrderByStopOrderAsc(id)) {
            if (rows.size() >= MAX_ROWS) break;
            rows.add(stop(stop));
        }
        result.put("stops", rows); result.put("truncated", rows.size() == MAX_ROWS); return result;
    }

    private Map<String, Object> passenger(Map<String, Object> args) {
        Long current = SecurityUtils.currentPassengerIdOrNull();
        Long requested = longValue(args.get("passengerId"));
        if (current == null || (requested != null && !current.equals(requested))) throw new ForbiddenException("Passenger scope");
        Passenger passenger = passengers.findById(current).orElseThrow();
        return Map.of("passengerId", passenger.getId(), "fullName", passenger.getFullName(), "phoneNumber", passenger.getPhoneNumber());
    }

    private Map<String, Object> bookings(long tripId, int limit) {
        Long passengerId = SecurityUtils.currentPassengerIdOrNull();
        if (passengerId == null) throw new ForbiddenException("Passenger scope");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (BoardingRequest request : requests.findByPassengerIdOrderByRequestedAtDesc(passengerId)) {
            if (request.getTrip() == null || request.getTrip().getId() != tripId || rows.size() >= limit) continue;
            Map<String, Object> row = new LinkedHashMap<>(); row.put("id", request.getId()); row.put("status", request.getStatus());
            row.put("boardingStop", stop(request.getBoardingStop())); row.put("destinationStop", stop(request.getDestinationStop()));
            row.put("requestedAt", request.getRequestedAt()); rows.add(row);
        }
        return Map.of("tripId", tripId, "bookings", rows);
    }

    private Map<String, Object> checkins(long tripId, int limit) {
        Trip trip = authorizedTrip(tripId, Map.of());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PassengerRecord record : records.findByTripIdOrderByRecordedAtAsc(trip.getId())) {
            if (rows.size() >= limit) break;
            Map<String, Object> row = new LinkedHashMap<>(); row.put("recordId", record.getId()); row.put("count", record.getPassengerCount());
            row.put("stop", stop(record.getStop())); row.put("boardingStop", stop(record.getBoardingStop()));
            row.put("source", record.getSource()); row.put("recordedAt", record.getRecordedAt()); rows.add(row);
        }
        return Map.of("tripId", tripId, "records", rows);
    }

    private Map<String, Object> syncStatus(Map<String, Object> context) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contextSource", context.get("contextSource")); result.put("clientVersion", context.get("clientVersion"));
        result.put("lastLocalUpdateAt", context.get("lastLocalUpdateAt")); result.put("syncState", context.get("syncState"));
        result.put("serverContextAvailable", !context.isEmpty()); return result;
    }

    private Map<String, Object> search(Map<String, Object> args, Map<String, Object> context, int limit) {
        String query = String.valueOf(args.getOrDefault("query", "")).toLowerCase(java.util.Locale.ROOT);
        List<Map<String, Object>> matches = new ArrayList<>();
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (String.valueOf(entry.getValue()).toLowerCase(java.util.Locale.ROOT).contains(query)) {
                Map<String, Object> match = new LinkedHashMap<>();
                match.put("field", entry.getKey()); match.put("value", entry.getValue()); matches.add(match);
                if (matches.size() >= limit) break;
            }
        }
        return Map.of("query", query, "matches", matches, "source", "AUTHORIZED_CLIENT_CONTEXT");
    }

    private Trip authorizedTrip(long id, Map<String, Object> context) {
        Trip trip = trips.findByIdWithDetails(id).orElseThrow();
        Long driverId = SecurityUtils.currentDriverIdOrNull();
        Long passengerId = SecurityUtils.currentPassengerIdOrNull();
        if (driverId != null && trip.getDriver() != null && driverId.equals(trip.getDriver().getId())) return trip;
        if (passengerId != null && requests.existsByPassengerIdAndTripId(passengerId, id)) return trip;
        throw new ForbiddenException("Trip scope");
    }

    private static Map<String, Object> stop(Stop stop) {
        if (stop == null) return null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", stop.getId()); result.put("name", stop.getName()); result.put("order", stop.getStopOrder());
        result.put("latitude", stop.getLatitude()); result.put("longitude", stop.getLongitude());
        return result;
    }
    private static Map<String, Object> error(String message) { return Map.of("error", message); }
    private static int limit(Map<String, Object> args) { return Math.max(1, Math.min(MAX_ROWS, intValue(args.get("limit"), 10))); }
    private static long id(Map<String, Object> args, String key, Map<String, Object> context, String... fallback) {
        Long value = longValue(args.get(key));
        if (value != null) return value;
        for (String candidate : fallback) { value = longValue(context.get(candidate)); if (value != null) return value; }
        throw new IllegalArgumentException("Missing " + key);
    }
    private static Long longValue(Object raw) { try { return raw == null ? null : Long.valueOf(String.valueOf(raw)); } catch (NumberFormatException e) { return null; } }
    private static int intValue(Object raw, int fallback) { try { return raw == null ? fallback : Integer.parseInt(String.valueOf(raw)); } catch (NumberFormatException e) { return fallback; } }
    private static Map<String, Object> function(String name, String description, Map<String, Object> parameters) {
        return Map.of("type", "function", "function", Map.of("name", name, "description", description, "parameters", parameters));
    }
    private static Map<String, Object> object(List<String> required, Map<String, Object> properties) {
        return Map.of("type", "object", "required", required, "properties", properties);
    }
    private static Map<String, Object> string() { return Map.of("type", "string"); }
    private static Map<String, Object> number() { return Map.of("type", "integer"); }
    private static Map<String, Object> integer() { return Map.of("type", "integer", "minimum", 1, "maximum", MAX_ROWS); }
}
