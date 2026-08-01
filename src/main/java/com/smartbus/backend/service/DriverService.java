package com.smartbus.backend.service;

import com.smartbus.backend.dto.DriverResponse;

public interface DriverService {

    DriverResponse getCurrentDriver();

    DriverResponse getById(Long id);
}
