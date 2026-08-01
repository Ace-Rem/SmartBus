package com.smartbus.backend.service.impl;

import com.smartbus.backend.dto.DriverResponse;
import com.smartbus.backend.entity.Driver;
import com.smartbus.backend.exception.ForbiddenException;
import com.smartbus.backend.exception.ResourceNotFoundException;
import com.smartbus.backend.mapper.DriverMapper;
import com.smartbus.backend.repository.DriverRepository;
import com.smartbus.backend.security.SecurityUtils;
import com.smartbus.backend.service.DriverService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;

    public DriverServiceImpl(DriverRepository driverRepository, DriverMapper driverMapper) {
        this.driverRepository = driverRepository;
        this.driverMapper = driverMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public DriverResponse getCurrentDriver() {
        Long driverId = SecurityUtils.requireCurrentDriverId();
        return getById(driverId);
    }

    @Override
    @Transactional(readOnly = true)
    public DriverResponse getById(Long id) {
        Long currentDriverId = SecurityUtils.requireCurrentDriverId();
        if (!currentDriverId.equals(id)) {
            throw new ForbiddenException("Cannot access another driver's profile");
        }
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + id));
        return driverMapper.toResponse(driver);
    }
}
