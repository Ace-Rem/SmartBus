package com.smartbus.backend.service.impl;

import com.smartbus.backend.dto.FastBoardingSignalRequest;
import com.smartbus.backend.dto.FastBoardingSignalResponse;
import com.smartbus.backend.exception.BadRequestException;
import com.smartbus.backend.security.SecurityUtils;
import com.smartbus.backend.service.FastBoardingSignalService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class FastBoardingSignalServiceImpl implements FastBoardingSignalService {

    private static final long SIGNAL_TTL_MILLIS = 120_000L;

    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentLinkedQueue<PendingSignal> pendingSignals = new ConcurrentLinkedQueue<>();

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
                    && signal.destinationStopId.equals(request.getDestinationStopId())) {
                return signal.response();
            }
        }

        PendingSignal signal = new PendingSignal(
                sequence.incrementAndGet(),
                passengerId,
                request.getRouteId(),
                request.getDestinationStopId(),
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

    private void cleanup(long now) {
        pendingSignals.removeIf(signal -> now - signal.createdAt > SIGNAL_TTL_MILLIS);
    }

    private static final class PendingSignal {

        private final long id;
        private final long passengerId;
        private final Long routeId;
        private final Long destinationStopId;
        private final long createdAt;

        private PendingSignal(
                long id,
                long passengerId,
                Long routeId,
                Long destinationStopId,
                long createdAt
        ) {
            this.id = id;
            this.passengerId = passengerId;
            this.routeId = routeId;
            this.destinationStopId = destinationStopId;
            this.createdAt = createdAt;
        }

        private FastBoardingSignalResponse response() {
            return new FastBoardingSignalResponse(id, routeId, destinationStopId);
        }
    }
}
