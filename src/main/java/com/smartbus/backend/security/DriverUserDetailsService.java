package com.smartbus.backend.security;

import com.smartbus.backend.repository.DriverRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DriverUserDetailsService implements UserDetailsService {

    private final DriverRepository driverRepository;

    public DriverUserDetailsService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return driverRepository.findByUsername(username)
                .map(driver -> new DriverPrincipal(
                        driver.getId(),
                        driver.getUsername(),
                        driver.getPasswordHash(),
                        Boolean.TRUE.equals(driver.getActive())
                ))
                .orElseThrow(() -> new UsernameNotFoundException("Driver not found: " + username));
    }
}
