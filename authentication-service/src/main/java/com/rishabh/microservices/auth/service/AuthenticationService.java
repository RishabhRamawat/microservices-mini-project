package com.rishabh.microservices.auth.service;

import com.rishabh.microservices.auth.dto.AuthResponseDto;
import com.rishabh.microservices.auth.dto.LoginRequestDto;
import com.rishabh.microservices.auth.dto.MessageResponseDto;
import com.rishabh.microservices.auth.dto.RegisterRequestDto;
import com.rishabh.microservices.auth.dto.ResendOtpRequestDto;
import com.rishabh.microservices.auth.dto.SetPasswordRequestDto;
import com.rishabh.microservices.auth.dto.VerifyOtpRequestDto;

public interface AuthenticationService {

    MessageResponseDto register(RegisterRequestDto request);

    MessageResponseDto verifyOtp(VerifyOtpRequestDto request);

    MessageResponseDto resendOtp(ResendOtpRequestDto request);

    MessageResponseDto setPassword(SetPasswordRequestDto request);

    AuthResponseDto login(LoginRequestDto request);
}



