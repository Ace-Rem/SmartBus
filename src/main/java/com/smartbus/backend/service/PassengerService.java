package com.smartbus.backend.service;

import com.smartbus.backend.dto.PassengerLoginRequest;
import com.smartbus.backend.dto.PassengerLoginResponse;
import com.smartbus.backend.dto.PassengerRegisterRequest;
import com.smartbus.backend.dto.PassengerResponse;

public interface PassengerService {

    PassengerLoginResponse register(PassengerRegisterRequest request);

    PassengerLoginResponse login(PassengerLoginRequest request);

    PassengerResponse getMe();
}
