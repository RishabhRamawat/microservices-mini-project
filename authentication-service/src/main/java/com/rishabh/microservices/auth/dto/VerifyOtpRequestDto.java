package com.rishabh.microservices.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Data transfer object for verifying an email identity using a provided OTP.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyOtpRequestDto {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String otp;
}
