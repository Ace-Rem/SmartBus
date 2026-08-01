package com.smartbus.backend.mapper;

import com.smartbus.backend.dto.DriverResponse;
import com.smartbus.backend.entity.Driver;
import org.springframework.stereotype.Component;

@Component
public class DriverMapper {

    public DriverResponse toResponse(Driver driver) {
        if (driver == null) {
            return null;
        }
        DriverResponse response = new DriverResponse();
        response.setId(driver.getId());
        response.setFullName(driver.getFullName());
        response.setPhoneNumber(driver.getPhoneNumber());
        response.setLicenseNumber(driver.getLicenseNumber());
        return response;
    }
}
