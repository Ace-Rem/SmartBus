package com.smartbus.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "smartbus.gps")
public class GpsProperties {

    /**
     * Distance in meters within which a bus is considered at a stop.
     */
    private double stopProximityMeters = 50.0;

    public double getStopProximityMeters() {
        return stopProximityMeters;
    }

    public void setStopProximityMeters(double stopProximityMeters) {
        this.stopProximityMeters = stopProximityMeters;
    }
}
