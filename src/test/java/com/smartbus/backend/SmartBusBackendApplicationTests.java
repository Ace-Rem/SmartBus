package com.smartbus.backend;

import com.smartbus.backend.repository.DriverRepository;
import com.smartbus.backend.repository.PassengerRecordRepository;
import com.smartbus.backend.repository.RouteRepository;
import com.smartbus.backend.repository.StopRepository;
import com.smartbus.backend.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "security.jwt.secret=test-secret-key-at-least-32-characters-long"
})
class SmartBusBackendApplicationTests {

    @MockBean
    private DriverRepository driverRepository;

    @MockBean
    private RouteRepository routeRepository;

    @MockBean
    private StopRepository stopRepository;

    @MockBean
    private TripRepository tripRepository;

    @MockBean
    private PassengerRecordRepository passengerRecordRepository;

    @Test
    void contextLoads() {
    }
}
