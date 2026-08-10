package com.smartbus.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.smartbus.backend.entity.Passenger;
import com.smartbus.backend.repository.PassengerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final DriverUserDetailsService driverUserDetailsService;
    private final PassengerRepository passengerRepository;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            DriverUserDetailsService driverUserDetailsService,
            PassengerRepository passengerRepository
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.driverUserDetailsService = driverUserDetailsService;
        this.passengerRepository = passengerRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtTokenProvider.validateToken(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    String username = jwtTokenProvider.getSubject(token);
                    UserDetails userDetails = resolvePrincipal(token, username);
                    if (!userDetails.isEnabled()) {
                        log.debug("Rejected JWT for disabled account: {}", username);
                    } else {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (UsernameNotFoundException ex) {
                    log.debug("JWT subject not found; leaving request unauthenticated");
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private UserDetails resolvePrincipal(String token, String username) {
        Long passengerId = jwtTokenProvider.getPassengerId(token);
        if (passengerId != null) {
            Passenger passenger = passengerRepository.findById(passengerId)
                    .orElseThrow(() -> new UsernameNotFoundException("Passenger not found"));
            return new PassengerPrincipal(
                    passenger.getId(),
                    passenger.getUsername(),
                    passenger.getPasswordHash(),
                    Boolean.TRUE.equals(passenger.getActive()),
                    passenger.getFullName(),
                    passenger.getPhoneNumber()
            );
        }
        return driverUserDetailsService.loadUserByUsername(username);
    }
}
