package com.rishabh.microservices.auth.controller;

import com.rishabh.microservices.auth.dto.AuthResponseDto;
import com.rishabh.microservices.auth.dto.LoginRequestDto;
import com.rishabh.microservices.auth.dto.MessageResponseDto;
import com.rishabh.microservices.auth.dto.RegisterRequestDto;
import com.rishabh.microservices.auth.dto.ResendOtpRequestDto;
import com.rishabh.microservices.auth.dto.SetPasswordRequestDto;
import com.rishabh.microservices.auth.dto.VerifyOtpRequestDto;
import com.rishabh.microservices.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// REST controller exposing public authentication endpoints.
// Pure delegation layer — contains zero business logic, hashing, or database access.
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponseDto register(@Valid @RequestBody RegisterRequestDto request) {
        return authenticationService.register(request);
    }

    @PostMapping("/verify-otp")
    @ResponseStatus(HttpStatus.OK)
    public MessageResponseDto verifyOtp(@Valid @RequestBody VerifyOtpRequestDto request) {
        return authenticationService.verifyOtp(request);
    }

    @PostMapping("/resend-otp")
    @ResponseStatus(HttpStatus.OK)
    public MessageResponseDto resendOtp(@Valid @RequestBody ResendOtpRequestDto request) {
        return authenticationService.resendOtp(request);
    }

    @PostMapping("/set-password")
    @ResponseStatus(HttpStatus.OK)
    public MessageResponseDto setPassword(@Valid @RequestBody SetPasswordRequestDto request) {
        return authenticationService.setPassword(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponseDto login(@Valid @RequestBody LoginRequestDto request) {
        return authenticationService.login(request);
    }
}
