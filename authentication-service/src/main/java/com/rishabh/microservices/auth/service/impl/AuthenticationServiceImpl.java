package com.rishabh.microservices.auth.service.impl;

import com.rishabh.microservices.auth.dto.AuthResponseDto;
import com.rishabh.microservices.auth.dto.LoginRequestDto;
import com.rishabh.microservices.auth.dto.MessageResponseDto;
import com.rishabh.microservices.auth.dto.RegisterRequestDto;
import com.rishabh.microservices.auth.dto.ResendOtpRequestDto;
import com.rishabh.microservices.auth.dto.SetPasswordRequestDto;
import com.rishabh.microservices.auth.dto.VerifyOtpRequestDto;
import com.rishabh.microservices.auth.entity.Identity;
import com.rishabh.microservices.auth.entity.OtpVerification;
import com.rishabh.microservices.auth.enums.Role;
import com.rishabh.microservices.auth.exception.EmailNotVerifiedException;
import com.rishabh.microservices.auth.exception.IdentityAlreadyExistsException;
import com.rishabh.microservices.auth.exception.IdentityAlreadyVerifiedException;
import com.rishabh.microservices.auth.exception.IdentityNotFoundException;
import com.rishabh.microservices.auth.exception.InvalidCredentialsException;
import com.rishabh.microservices.auth.exception.OtpExpiredException;
import com.rishabh.microservices.auth.exception.OtpInvalidException;
import com.rishabh.microservices.auth.exception.OtpNotFoundException;
import com.rishabh.microservices.auth.exception.PasswordAlreadySetException;
import com.rishabh.microservices.auth.exception.PasswordMismatchException;
import com.rishabh.microservices.auth.exception.PasswordNotConfiguredException;
import com.rishabh.microservices.auth.repository.IdentityRepository;
import com.rishabh.microservices.auth.repository.OtpVerificationRepository;
import com.rishabh.microservices.auth.security.CustomUserDetails;
import com.rishabh.microservices.auth.security.JwtService;
import com.rishabh.microservices.auth.service.AuthenticationService;
import com.rishabh.microservices.auth.service.EmailService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int OTP_MIN = 100_000;
    private static final int OTP_MAX = 999_999;

    private final IdentityRepository identityRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    // Shared BCrypt encoder for OTP hashing and password hashing
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthenticationServiceImpl(IdentityRepository identityRepository,

                                     OtpVerificationRepository otpVerificationRepository,
                                     BCryptPasswordEncoder passwordEncoder,
                                     SecureRandom secureRandom,
                                     AuthenticationManager authenticationManager,
                                     JwtService jwtService,
                                     EmailService emailService) {
        this.identityRepository = identityRepository;
        this.otpVerificationRepository = otpVerificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = secureRandom;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public MessageResponseDto register(RegisterRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (identityRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IdentityAlreadyExistsException("An account with this email already exists.");
        }

        LocalDateTime now = LocalDateTime.now();

        Identity identity = new Identity();
        identity.setUserId(UUID.randomUUID().toString());
        identity.setEmail(normalizedEmail);
        identity.setPasswordHash(null);
        identity.setEmailVerified(false);
        identity.setRole(Role.USER);
        identity.setCreatedAt(now);
        identity.setUpdatedAt(now);

        String rawOtp = generateOtp();
        String otpHash = passwordEncoder.encode(rawOtp);
        // rawOtp is discarded after hashing; it must never be stored or logged

        OtpVerification otpVerification = new OtpVerification();
        otpVerification.setIdentity(identity);
        otpVerification.setOtpHash(otpHash);
        otpVerification.setExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES));
        otpVerification.setVerified(false);
        otpVerification.setCreatedAt(now);

        // Keep both sides of the bidirectional relationship consistent
        identity.getOtpVerifications().add(otpVerification);

        // CascadeType.ALL on Identity.otpVerifications persists both atomically
        identityRepository.save(identity);

        // Send generated raw OTP via Brevo transactional email; rawOtp is never stored
        emailService.sendOtpEmail(normalizedEmail, rawOtp);

        return MessageResponseDto.builder()
                .message("Registration successful. Please verify your email.")
                .build();
    }

    @Override
    @Transactional
    public MessageResponseDto verifyOtp(VerifyOtpRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        Identity identity = identityRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IdentityNotFoundException("Identity not found."));

        if (identity.isEmailVerified()) {
            throw new IdentityAlreadyVerifiedException("Email is already verified.");
        }

        // Only the most recently issued unverified OTP is eligible; older records are ignored
        OtpVerification otp = otpVerificationRepository
                .findTopByIdentityAndVerifiedFalseOrderByCreatedAtDesc(identity)
                .orElseThrow(() -> new OtpNotFoundException("No active OTP found."));

        LocalDateTime now = LocalDateTime.now();

        if (otp.getExpiresAt().isBefore(now)) {
            throw new OtpExpiredException("OTP has expired.");
        }

        // Use matches() — never re-encode the raw OTP for comparison
        if (!passwordEncoder.matches(request.getOtp(), otp.getOtpHash())) {
            throw new OtpInvalidException("Invalid OTP.");
        }

        otp.setVerified(true);
        identity.setEmailVerified(true);
        identity.setUpdatedAt(now);

        // CascadeType.ALL propagates the OTP state change through the identity save
        identityRepository.save(identity);

        return MessageResponseDto.builder()
                .message("Email verified successfully.")
                .build();
    }

    @Override
    @Transactional
    public MessageResponseDto resendOtp(ResendOtpRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        Identity identity = identityRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IdentityNotFoundException("Identity not found."));

        if (identity.isEmailVerified()) {
            throw new IdentityAlreadyVerifiedException("Email is already verified.");
        }

        LocalDateTime now = LocalDateTime.now();

        String rawOtp = generateOtp();
        String otpHash = passwordEncoder.encode(rawOtp);
        // rawOtp is discarded after hashing; it must never be stored or logged

        OtpVerification newOtp = new OtpVerification();
        newOtp.setIdentity(identity);
        newOtp.setOtpHash(otpHash);
        newOtp.setVerified(false);
        newOtp.setCreatedAt(now);
        newOtp.setExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES));

        // Adding to the collection lets CascadeType.ALL persist the new OTP atomically
        identity.getOtpVerifications().add(newOtp);
        identityRepository.save(identity);

        // Send generated raw OTP via Brevo transactional email; rawOtp is never stored
        emailService.sendOtpEmail(normalizedEmail, rawOtp);

        return MessageResponseDto.builder()
                .message("A new OTP has been sent to your email.")
                .build();
    }


    @Override
    @Transactional
    public MessageResponseDto setPassword(SetPasswordRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        Identity identity = identityRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IdentityNotFoundException("Identity not found."));

        if (!identity.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email must be verified before setting a password.");
        }

        // Guard: setPassword is a one-time operation; use a dedicated flow for password change
        if (identity.getPasswordHash() != null) {
            throw new PasswordAlreadySetException("A password has already been set for this account.");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("Password and confirmation do not match.");
        }

        // Raw password is discarded immediately after encoding; never stored or logged
        identity.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        identity.setUpdatedAt(LocalDateTime.now());

        identityRepository.save(identity);

        return MessageResponseDto.builder()
                .message("Password set successfully.")
                .build();
    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        Identity identity = identityRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IdentityNotFoundException("Identity not found."));

        if (!identity.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email must be verified before logging in.");
        }

        if (identity.getPasswordHash() == null) {
            throw new PasswordNotConfiguredException("Password has not been configured for this account.");
        }

        try {
            // DaoAuthenticationProvider performs BCrypt verification; no manual comparison needed here.
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword()));

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String token = jwtService.generateToken(userDetails);

            return AuthResponseDto.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .userId(identity.getUserId())
                    .email(identity.getEmail())
                    .role(identity.getRole())
                    .build();

        } catch (BadCredentialsException e) {
            // Wrap Spring Security exception into domain exception for consistent error handling.
            throw new InvalidCredentialsException("Invalid email or password.");
        }
    }

    // Normalisation ensures consistent lookup regardless of client-supplied casing or whitespace
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String generateOtp() {
        int otp = OTP_MIN + secureRandom.nextInt(OTP_MAX - OTP_MIN + 1);
        return String.valueOf(otp);
    }
}
