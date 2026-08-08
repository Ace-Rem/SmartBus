package com.smartbus.backend.security;

import com.smartbus.backend.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static DriverPrincipal requireCurrentDriver() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof DriverPrincipal principal)) {
            throw new UnauthorizedException("Driver authentication required");
        }
        return principal;
    }

    public static Long requireCurrentDriverId() {
        return requireCurrentDriver().getId();
    }

    public static PassengerPrincipal requireCurrentPassenger() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof PassengerPrincipal principal)) {
            throw new UnauthorizedException("Passenger authentication required");
        }
        return principal;
    }

    public static Long requireCurrentPassengerId() {
        return requireCurrentPassenger().getId();
    }
}
