package com.smartbus.backend.controller;

import com.smartbus.backend.security.DriverPrincipal;
import com.smartbus.backend.security.PassengerPrincipal;
import io.ably.lib.rest.AblyRest;
import io.ably.lib.rest.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Exchanges the existing SmartBus JWT for a short-lived, scoped Ably token. */
@RestController
@RequestMapping("/ably")
@Tag(name = "Ably")
@SecurityRequirement(name = "bearerAuth")
public class AblyTokenController {

    private final String apiKey;
    private final String channel;

    public AblyTokenController(
            @Value("${ably.api-key:}") String apiKey,
            @Value("${ably.channel:smartbus/check-ins}") String channel
    ) {
        this.apiKey = apiKey;
        this.channel = channel;
    }

    @GetMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Issue a scoped Ably token for the signed-in app")
    public ResponseEntity<String> token() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Ably token service is not configured"
            );
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Auth.TokenParams params = new Auth.TokenParams();
        params.ttl = 60L * 60L * 1000L;
        if (authentication != null && authentication.getPrincipal() instanceof PassengerPrincipal passenger) {
            params.clientId = "passenger:" + passenger.getId();
            params.capability = capability("publish");
        } else if (authentication != null && authentication.getPrincipal() instanceof DriverPrincipal driver) {
            params.clientId = "driver:" + driver.getId();
            params.capability = capability("subscribe");
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SmartBus authentication required");
        }
        try (AblyRest ably = new AblyRest(apiKey)) {
            Auth.TokenDetails details = ably.auth.requestToken(params, null);
            return ResponseEntity.ok(details.asJson());
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to issue Ably token",
                    exception
            );
        }
    }

    private String capability(String operation) {
        return "{\"" + channel.replace("\"", "") + "\":[\"" + operation + "\"]}";
    }
}
