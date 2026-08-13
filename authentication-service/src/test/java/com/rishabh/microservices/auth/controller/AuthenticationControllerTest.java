package com.rishabh.microservices.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishabh.microservices.auth.dto.AuthResponseDto;
import com.rishabh.microservices.auth.dto.LoginRequestDto;
import com.rishabh.microservices.auth.dto.MessageResponseDto;
import com.rishabh.microservices.auth.dto.RegisterRequestDto;
import com.rishabh.microservices.auth.dto.ResendOtpRequestDto;
import com.rishabh.microservices.auth.dto.SetPasswordRequestDto;
import com.rishabh.microservices.auth.dto.VerifyOtpRequestDto;
import com.rishabh.microservices.auth.enums.Role;
import com.rishabh.microservices.auth.exception.EmailNotVerifiedException;
import com.rishabh.microservices.auth.exception.IdentityAlreadyVerifiedException;
import com.rishabh.microservices.auth.exception.IdentityNotFoundException;
import com.rishabh.microservices.auth.exception.InvalidCredentialsException;
import com.rishabh.microservices.auth.exception.OtpExpiredException;
import com.rishabh.microservices.auth.exception.OtpInvalidException;
import com.rishabh.microservices.auth.exception.PasswordAlreadySetException;
import com.rishabh.microservices.auth.exception.PasswordMismatchException;
import com.rishabh.microservices.auth.exception.PasswordNotConfiguredException;
import com.rishabh.microservices.auth.security.JwtAuthenticationEntryPoint;
import com.rishabh.microservices.auth.security.JwtAuthenticationFilter;
import com.rishabh.microservices.auth.security.JwtService;
import com.rishabh.microservices.auth.service.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false) // Add filters = false tests controller + ExceptionHandler mapping directly
@DisplayName("AuthenticationController Unit Tests")
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("1. Register endpoint returns 201 Created and MessageResponseDto")
    void register_returns201Created() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder().email("user@example.com").build();
        MessageResponseDto response = MessageResponseDto.builder().message("Registration successful.").build();

        when(authenticationService.register(any())).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful."));
    }

    @Test
    @DisplayName("2. Verify OTP endpoint returns 200 OK and MessageResponseDto")
    void verifyOtp_returns200Ok() throws Exception {
        VerifyOtpRequestDto request = VerifyOtpRequestDto.builder().email("user@example.com").otp("123456").build();
        MessageResponseDto response = MessageResponseDto.builder().message("Email verified successfully.").build();

        when(authenticationService.verifyOtp(any())).thenReturn(response);

        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully."));
    }

    @Test
    @DisplayName("3. Resend OTP endpoint returns 200 OK and MessageResponseDto")
    void resendOtp_returns200Ok() throws Exception {
        ResendOtpRequestDto request = ResendOtpRequestDto.builder().email("user@example.com").build();
        MessageResponseDto response = MessageResponseDto.builder().message("A new OTP has been sent.").build();

        when(authenticationService.resendOtp(any())).thenReturn(response);

        mockMvc.perform(post("/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("A new OTP has been sent."));
    }

    @Test
    @DisplayName("4. Set password endpoint returns 200 OK and MessageResponseDto")
    void setPassword_returns200Ok() throws Exception {
        SetPasswordRequestDto request = SetPasswordRequestDto.builder()
                .email("user@example.com")
                .password("Pass123!")
                .confirmPassword("Pass123!")
                .build();
        MessageResponseDto response = MessageResponseDto.builder().message("Password set successfully.").build();

        when(authenticationService.setPassword(any())).thenReturn(response);

        mockMvc.perform(post("/auth/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password set successfully."));
    }

    @Test
    @DisplayName("5. Login endpoint returns 200 OK and AuthResponseDto")
    void login_returns200OkAndAuthResponse() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder().email("user@example.com").password("Pass123!").build();
        AuthResponseDto response = AuthResponseDto.builder()
                .token("jwt-token-string")
                .tokenType("Bearer")
                .userId("user-123")
                .email("user@example.com")
                .role(Role.USER)
                .build();

        when(authenticationService.login(any())).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-string"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value("user-123"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("6. Validation errors return 400 Bad Request")
    void validationError_returns400BadRequest() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder().email("invalid-email").build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("7. IdentityNotFoundException maps correctly to 404 Not Found")
    void identityNotFound_mapsTo404() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder().email("user@example.com").build();
        when(authenticationService.register(any())).thenThrow(new IdentityNotFoundException("Identity not found."));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Identity not found."));
    }

    @Test
    @DisplayName("8. Invalid OTP maps correctly to 400 Bad Request")
    void invalidOtp_mapsTo400() throws Exception {
        VerifyOtpRequestDto request = VerifyOtpRequestDto.builder().email("user@example.com").otp("123456").build();
        when(authenticationService.verifyOtp(any())).thenThrow(new OtpInvalidException("Invalid OTP."));

        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid OTP."));
    }

    @Test
    @DisplayName("9. Expired OTP maps correctly to 400 Bad Request")
    void expiredOtp_mapsTo400() throws Exception {
        VerifyOtpRequestDto request = VerifyOtpRequestDto.builder().email("user@example.com").otp("123456").build();
        when(authenticationService.verifyOtp(any())).thenThrow(new OtpExpiredException("OTP has expired."));

        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("OTP has expired."));
    }

    @Test
    @DisplayName("10. Already verified identity maps correctly to 409 Conflict")
    void alreadyVerified_mapsTo409() throws Exception {
        VerifyOtpRequestDto request = VerifyOtpRequestDto.builder().email("user@example.com").otp("123456").build();
        when(authenticationService.verifyOtp(any())).thenThrow(new IdentityAlreadyVerifiedException("Email is already verified."));

        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already verified."));
    }

    @Test
    @DisplayName("11. Password mismatch maps correctly to 400 Bad Request")
    void passwordMismatch_mapsTo400() throws Exception {
        SetPasswordRequestDto request = SetPasswordRequestDto.builder()
                .email("user@example.com")
                .password("Pass123!")
                .confirmPassword("Different!")
                .build();
        when(authenticationService.setPassword(any())).thenThrow(new PasswordMismatchException("Password and confirmation do not match."));

        mockMvc.perform(post("/auth/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password and confirmation do not match."));
    }

    @Test
    @DisplayName("12. Already-set password maps correctly to 409 Conflict")
    void passwordAlreadySet_mapsTo409() throws Exception {
        SetPasswordRequestDto request = SetPasswordRequestDto.builder()
                .email("user@example.com")
                .password("Pass123!")
                .confirmPassword("Pass123!")
                .build();
        when(authenticationService.setPassword(any())).thenThrow(new PasswordAlreadySetException("A password has already been set."));

        mockMvc.perform(post("/auth/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A password has already been set."));
    }

    @Test
    @DisplayName("13. Invalid credentials return 401 Unauthorized")
    void invalidCredentials_returns401() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder().email("user@example.com").password("WrongPass").build();
        when(authenticationService.login(any())).thenThrow(new InvalidCredentialsException("Invalid email or password."));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    @DisplayName("14. Unverified identity login is rejected with 403 Forbidden")
    void unverifiedIdentityLogin_returns403() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder().email("user@example.com").password("Pass123!").build();
        when(authenticationService.login(any())).thenThrow(new EmailNotVerifiedException("Email must be verified before logging in."));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Email must be verified before logging in."));
    }

    @Test
    @DisplayName("15. Password-not-configured login is rejected with 400 Bad Request")
    void passwordNotConfiguredLogin_returns400() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder().email("user@example.com").password("Pass123!").build();
        when(authenticationService.login(any())).thenThrow(new PasswordNotConfiguredException("Password has not been configured."));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password has not been configured."));
    }

    @Test
    @DisplayName("18. Error responses do not expose passwords, hashes, OTPs, or internal details")
    void errorResponse_doesNotExposeSensitiveData() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder().email("user@example.com").password("SecretPass123").build();
        when(authenticationService.login(any())).thenThrow(new InvalidCredentialsException("Invalid email or password."));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(not(containsString("SecretPass123"))))
                .andExpect(jsonPath("$.message").value(not(containsString("$2a$"))))
                .andExpect(jsonPath("$.message").value(not(containsString("123456"))))
                .andExpect(jsonPath("$.message").value(not(containsString("jwt-token"))));
    }
}