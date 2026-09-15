package com.smartbus.backend.service.impl;

import com.smartbus.backend.dto.FastBoardingSignalRequest;
import com.smartbus.backend.dto.FastBoardingSignalAcceptRequest;
import com.smartbus.backend.dto.FastBoardingAcceptanceResponse;
import com.smartbus.backend.dto.FastBoardingSignalCancelRequest;
import com.smartbus.backend.dto.FastBoardingSignalResponse;
import com.smartbus.backend.exception.BadRequestException;
import com.smartbus.backend.exception.ForbiddenException;
import com.smartbus.backend.exception.ResourceNotFoundException;
import com.smartbus.backend.entity.Trip;
import com.smartbus.backend.security.SecurityUtils;
import com.smartbus.backend.repository.TripRepository;
import com.smartbus.backend.util.TripStatus;
import com.smartbus.backend.service.FastBoardingSignalService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class FastBoardingSignalServiceImpl implements FastBoardingSignalService {

    private static final long SIGNAL_TTL_MILLIS = 120_000L;
    private static final long ACCEPTANCE_TTL_MILLIS = 600_000L;

    private final AtomicLong sequence = new AtomicLong();
    private final AtomicLong acceptanceSequence = new AtomicLong();
    private final AtomicLong cancellationSequence = new AtomicLong();
    private final ConcurrentLinkedQueue<PendingSignal> pendingSignals = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<AcceptedSignal> acceptedSignals = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<CancelledSignal> cancelledSignals = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Long, AcceptedSignal> acceptedBySignal = new ConcurrentHashMap<>();
    private final TripRepository tripRepository;

    public FastBoardingSignalServiceImpl(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Override
    public FastBoardingSignalResponse publish(FastBoardingSignalRequest request) {
        Long passengerId = SecurityUtils.requireCurrentPassengerId();
        if (request == null || request.getRouteId() == null || request.getDestinationStopId() == null) {
            throw new BadRequestException("routeId and destinationStopId are required");
        }

        long now = Instant.now().toEpochMilli();
        cleanup(now);
        for (PendingSignal signal : pendingSignals) {
            if (signal.passengerId == passengerId
                    && signal.routeId.equals(request.getRouteId())
                    && signal.destinationStopId.equals(request.getDestinationStopId())
                    && sameOptional(signal.tripId, request.getTripId())
                    && sameOptional(signal.boardingStopId, request.getBoardingStopId())) {
                return signal.response();
            }
        }

        PendingSignal signal = new PendingSignal(
                sequence.incrementAndGet(),
                passengerId,
                request.getRouteId(),
                request.getTripId(),
                request.getBoardingStopId(),
                request.getDestinationStopId(),
                request.getPassengerIdentifier(),
                now
        );
        pendingSignals.add(signal);
        return signal.response();
    }

    @Override
    public List<FastBoardingSignalResponse> poll(Long routeId, Long afterId) {
        SecurityUtils.requireCurrentDriverId();
        if (routeId == null || routeId <= 0) {
            throw new BadRequestException("routeId is required");
        }
        long cursor = afterId == null || afterId < 0 ? 0L : afterId;
        cleanup(Instant.now().toEpochMilli());
        List<FastBoardingSignalResponse> result = new ArrayList<>();
        for (PendingSignal signal : pendingSignals) {
            if (signal.routeId.equals(routeId) && signal.id > cursor) {
                result.add(signal.response());
            }
        }
        return result;
    }

    @Override
    public FastBoardingAcceptanceResponse accept(Long signalId, FastBoardingSignalAcceptRequest request) {
        Long driverId = SecurityUtils.requireCurrentDriverId();
        cleanup(Instant.now().toEpochMilli());
        if (signalId != null) {
            AcceptedSignal existing = acceptedBySignal.get(signalId);
            if (existing != null) return existing.response();
        }
        PendingSignal pending = null;
        for (PendingSignal candidate : pendingSignals) {
            boolean matchesId = signalId != null && candidate.id == signalId;
            boolean matchesIdentity = signalId == null && request != null
                    && request.getRouteId() != null
                    && request.getPassengerIdentifier() != null
                    && candidate.routeId.equals(request.getRouteId())
                    && request.getPassengerIdentifier().equals(candidate.passengerIdentifier);
            if (matchesId || matchesIdentity) {
                pending = candidate;
                break;
            }
        }
        if (pending == null) {
            throw new ResourceNotFoundException("Fast boarding signal not found: " + signalId);
        }
        Long tripId = request == null ? null : request.getTripId();
        if (tripId == null) {
            tripId = pending.tripId;
        }
        if (tripId == null) {
            throw new BadRequestException("tripId is required when accepting a signal");
        }
        final Long resolvedTripId = tripId;
        Trip trip = tripRepository.findByIdWithDetails(resolvedTripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + resolvedTripId));
        if (trip.getDriver() == null || !driverId.equals(trip.getDriver().getId())) {
            throw new ForbiddenException("Trip does not belong to current driver");
        }
        if (!TripStatus.IN_PROGRESS.equals(trip.getStatus())) {
            throw new BadRequestException("Trip is not in progress");
        }
        if (trip.getRoute() == null || !pending.routeId.equals(trip.getRoute().getId())) {
            throw new BadRequestException("Signal route does not match trip route");
        }
        if (request != null && request.getPassengerIdentifier() != null
                && pending.passengerIdentifier != null
                && !request.getPassengerIdentifier().equals(pending.passengerIdentifier)) {
            throw new ForbiddenException("Passenger signal identity mismatch");
        }
        pendingSignals.remove(pending);
        AcceptedSignal accepted = new AcceptedSignal(
                acceptanceSequence.incrementAndGet(),
                pending.id,
                pending.passengerId,
                tripId,
                pending.routeId,
                pending.boardingStopId,
                pending.destinationStopId,
                Instant.now().toEpochMilli()
        );
        acceptedBySignal.put(pending.id, accepted);
        acceptedSignals.add(accepted);
        return accepted.response();
    }

    @Override
    public FastBoardingSignalResponse cancel(FastBoardingSignalCancelRequest request) {
        Long passengerId = SecurityUtils.requireCurrentPassengerId();
        if (request == null) {
            throw new BadRequestException("Cancellation data is required");
        }
        cleanup(Instant.now().toEpochMilli());
        PendingSignal pending = null;
        for (PendingSignal candidate : pendingSignals) {
            boolean matchesSignal = request.getSignalId() != null
                    && candidate.id == request.getSignalId();
            boolean matchesIdentity = request.getSignalId() == null
                    && request.getRouteId() != null
                    && request.getPassengerIdentifier() != null
                    && candidate.routeId.equals(request.getRouteId())
                    && request.getPassengerIdentifier().equals(candidate.passengerIdentifier)
                    && (request.getDestinationStopId() == null
                    || request.getDestinationStopId().equals(candidate.destinationStopId));
            if ((matchesSignal || matchesIdentity) && candidate.passengerId == passengerId) {
                pending = candidate;
                break;
            }
        }
        if (pending == null) {
            throw new ResourceNotFoundException("Fast boarding signal not found");
        }
        pendingSignals.remove(pending);
        CancelledSignal cancelled = new CancelledSignal(
                cancellationSequence.incrementAndGet(),
                pending.id,
                pending.passengerId,
                pending.routeId,
                pending.tripId,
                pending.destinationStopId,
                pending.passengerIdentifier,
                Instant.now().toEpochMilli()
        );
        cancelledSignals.add(cancelled);
        return cancelled.response();
    }

    @Override
    public List<FastBoardingAcceptanceResponse> pollAccepted(Long afterId) {
        Long passengerId = SecurityUtils.requireCurrentPassengerId();
        long cursor = afterId == null || afterId < 0 ? 0L : afterId;
        cleanup(Instant.now().toEpochMilli());
        List<FastBoardingAcceptanceResponse> result = new ArrayList<>();
        for (AcceptedSignal accepted : acceptedSignals) {
            if (accepted.id > cursor && accepted.passengerId == passengerId) {
                result.add(accepted.response());
            }
        }
        return result;
    }

    @Override
    public List<FastBoardingSignalResponse> pollCancelled(Long routeId, Long afterId) {
        SecurityUtils.requireCurrentDriverId();
        if (routeId == null || routeId <= 0) {
            throw new BadRequestException("routeId is required");
        }
        long cursor = afterId == null || afterId < 0 ? 0L : afterId;
        cleanup(Instant.now().toEpochMilli());
        List<FastBoardingSignalResponse> result = new ArrayList<>();
        for (CancelledSignal cancelled : cancelledSignals) {
            if (cancelled.id > cursor && routeId.equals(cancelled.routeId)) {
                result.add(cancelled.response());
            }
        }
        return result;
    }

    private void cleanup(long now) {
        pendingSignals.removeIf(signal -> now - signal.createdAt > SIGNAL_TTL_MILLIS);
        acceptedSignals.removeIf(signal -> {
            boolean expired = now - signal.acceptedAt > ACCEPTANCE_TTL_MILLIS;
            if (expired) acceptedBySignal.remove(signal.signalId, signal);
            return expired;
        });
        cancelledSignals.removeIf(signal -> now - signal.cancelledAt > ACCEPTANCE_TTL_MILLIS);
    }

    private boolean sameOptional(Object left, Object right) {
        return left == null || right == null || left.equals(right);
    }

    private static final class PendingSignal {

        private final long id;
        private final long passengerId;
        private final Long routeId;
        private final Long tripId;
        private final Long boardingStopId;
        private final Long destinationStopId;
        private final String passengerIdentifier;
        private final long createdAt;

        private PendingSignal(
                long id,
                long passengerId,
                Long routeId,
                Long tripId,
                Long boardingStopId,
                Long destinationStopId,
                String passengerIdentifier,
                long createdAt
        ) {
            this.id = id;
            this.passengerId = passengerId;
            this.routeId = routeId;
            this.tripId = tripId;
            this.boardingStopId = boardingStopId;
            this.destinationStopId = destinationStopId;
            this.passengerIdentifier = passengerIdentifier;
            this.createdAt = createdAt;
        }

        private FastBoardingSignalResponse response() {
            FastBoardingSignalResponse response = new FastBoardingSignalResponse(id, routeId, destinationStopId);
            response.setTripId(tripId);
            response.setBoardingStopId(boardingStopId);
            response.setPassengerId(passengerId);
            response.setPassengerIdentifier(passengerIdentifier);
            return response;
        }
    }

    private static final class AcceptedSignal {

        private final long id;
        private final long signalId;
        private final long passengerId;
        private final Long tripId;
        private final Long routeId;
        private final Long boardingStopId;
        private final Long destinationStopId;
        private final long acceptedAt;

        private AcceptedSignal(long id, long signalId, long passengerId, Long tripId, Long routeId,
                               Long boardingStopId, Long destinationStopId, long acceptedAt) {
            this.id = id;
            this.signalId = signalId;
            this.passengerId = passengerId;
            this.tripId = tripId;
            this.routeId = routeId;
            this.boardingStopId = boardingStopId;
            this.destinationStopId = destinationStopId;
            this.acceptedAt = acceptedAt;
        }

        private FastBoardingAcceptanceResponse response() {
            return new FastBoardingAcceptanceResponse(
                    id, signalId, passengerId, tripId, routeId, boardingStopId,
                    destinationStopId, "BOARDED", LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(acceptedAt), java.time.ZoneId.systemDefault()));
        }
    }

    private static final class CancelledSignal {

        private final long id;
        private final long signalId;
        private final long passengerId;
        private final Long routeId;
        private final Long tripId;
        private final Long destinationStopId;
        private final String passengerIdentifier;
        private final long cancelledAt;

        private CancelledSignal(long id, long signalId, long passengerId, Long routeId, Long tripId,
                                Long destinationStopId, String passengerIdentifier, long cancelledAt) {
            this.id = id;
            this.signalId = signalId;
            this.passengerId = passengerId;
            this.routeId = routeId;
            this.tripId = tripId;
            this.destinationStopId = destinationStopId;
            this.passengerIdentifier = passengerIdentifier;
            this.cancelledAt = cancelledAt;
        }

        private FastBoardingSignalResponse response() {
            FastBoardingSignalResponse response = new FastBoardingSignalResponse(
                    id, routeId, destinationStopId);
            response.setSignalId(signalId);
            response.setTripId(tripId);
            response.setPassengerId(passengerId);
            response.setPassengerIdentifier(passengerIdentifier);
            response.setStatus("CANCELLED");
            return response;
        }
    }
}
