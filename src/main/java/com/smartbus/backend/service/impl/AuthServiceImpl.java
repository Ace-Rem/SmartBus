package com.smartbus.backend.service.impl;

import com.smartbus.backend.dto.DriverResponse;
import com.smartbus.backend.dto.LoginRequest;
import com.smartbus.backend.dto.LoginResponse;
import com.smartbus.backend.entity.Driver;
import com.smartbus.backend.exception.UnauthorizedException;
import com.smartbus.backend.mapper.DriverMapper;
import com.smartbus.backend.repository.DriverRepository;
import com.smartbus.backend.security.JwtTokenProvider;
import com.smartbus.backend.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final DriverMapper driverMapper;

    public AuthServiceImpl(
            DriverRepository driverRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            DriverMapper driverMapper
    ) {
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.driverMapper = driverMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Driver driver = driverRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!Boolean.TRUE.equals(driver.getActive())) {
            throw new UnauthorizedException("Driver account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), driver.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(driver.getUsername(), driver.getId());
        DriverResponse driverResponse = driverMapper.toResponse(driver);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setTokenType("Bearer");
        response.setExpiresInMinutes(jwtTokenProvider.getExpirationMinutes());
        response.setDriver(driverResponse);
        return response;
    }
}
