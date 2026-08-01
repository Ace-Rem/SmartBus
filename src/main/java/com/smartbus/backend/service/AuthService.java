package com.smartbus.backend.service;

import com.smartbus.backend.dto.LoginRequest;
import com.smartbus.backend.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
