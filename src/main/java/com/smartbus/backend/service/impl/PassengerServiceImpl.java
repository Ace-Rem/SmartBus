package com.smartbus.backend.service.impl;

import com.smartbus.backend.dto.PassengerLoginRequest;
import com.smartbus.backend.dto.PassengerLoginResponse;
import com.smartbus.backend.dto.PassengerRegisterRequest;
import com.smartbus.backend.dto.PassengerResponse;
import com.smartbus.backend.entity.Passenger;
import com.smartbus.backend.exception.ConflictException;
import com.smartbus.backend.exception.ResourceNotFoundException;
import com.smartbus.backend.exception.UnauthorizedException;
import com.smartbus.backend.mapper.PassengerMapper;
import com.smartbus.backend.repository.PassengerRepository;
import com.smartbus.backend.security.JwtTokenProvider;
import com.smartbus.backend.security.PassengerPrincipal;
import com.smartbus.backend.security.SecurityUtils;
import com.smartbus.backend.service.PassengerService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PassengerServiceImpl implements PassengerService {

    private final PassengerRepository passengerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PassengerMapper passengerMapper;

    public PassengerServiceImpl(
            PassengerRepository passengerRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            PassengerMapper passengerMapper
    ) {
        this.passengerRepository = passengerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passengerMapper = passengerMapper;
    }

    @Override
    @Transactional
    public PassengerLoginResponse register(PassengerRegisterRequest request) {
        if (passengerRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Passenger username already exists");
        }
        if (passengerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ConflictException("Passenger phone number already exists");
        }

        Passenger passenger = new Passenger();
        passenger.setFullName(request.getFullName());
        passenger.setPhoneNumber(request.getPhoneNumber());
        passenger.setUsername(request.getUsername());
        passenger.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        passenger.setActive(true);
        Passenger saved = passengerRepository.save(passenger);
        return buildLoginResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PassengerLoginResponse login(PassengerLoginRequest request) {
        String identifier = request.getUsername() == null ? "" : request.getUsername().trim();
        Passenger passenger = passengerRepository.findByUsernameOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        if (!Boolean.TRUE.equals(passenger.getActive())
                || !passwordEncoder.matches(request.getPassword(), passenger.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }
        return buildLoginResponse(passenger);
    }

    @Override
    @Transactional(readOnly = true)
    public PassengerResponse getMe() {
        PassengerPrincipal principal = SecurityUtils.requireCurrentPassenger();
        if (principal.getFullName() != null || principal.getPhoneNumber() != null) {
            PassengerResponse response = new PassengerResponse();
            response.setId(principal.getId());
            response.setUsername(principal.getUsername());
            response.setFullName(principal.getFullName());
            response.setPhoneNumber(principal.getPhoneNumber());
            return response;
        }
        Passenger passenger = passengerRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found: " + principal.getId()));
        return passengerMapper.toResponse(passenger);
    }

    private PassengerLoginResponse buildLoginResponse(Passenger passenger) {
        PassengerLoginResponse response = new PassengerLoginResponse();
        response.setAccessToken(jwtTokenProvider.generatePassengerToken(passenger.getUsername(), passenger.getId()));
        response.setTokenType("Bearer");
        response.setExpiresInMinutes(jwtTokenProvider.getExpirationMinutes());
        response.setPassenger(passengerMapper.toResponse(passenger));
        return response;
    }
}
