package com.smartbus.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class UpdateLocationRequest {

    @NotNull
    @DecimalMin(value = "-90.0", inclusive = true, message = "latitude must be >= -90")
    @DecimalMax(value = "90.0", inclusive = true, message = "latitude must be <= 90")
    private BigDecimal latitude;

    @NotNull
    @DecimalMin(value = "-180.0", inclusive = true, message = "longitude must be >= -180")
    @DecimalMax(value = "180.0", inclusive = true, message = "longitude must be <= 180")
    private BigDecimal longitude;

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }
}
