package com.rishabh.microservices.auth.service;

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
import com.rishabh.microservices.auth.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationServiceImpl")
class AuthenticationServiceImplTest {

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Mock
    private com.rishabh.microservices.auth.security.JwtService jwtService;

    // Real encoder and SecureRandom; mocking BCrypt would undermine the security contract
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    private AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        // Inject real beans and mocks manually
        authenticationService = new AuthenticationServiceImpl(
                identityRepository, otpVerificationRepository, passwordEncoder, secureRandom, authenticationManager, jwtService);
    }

    // ─── Shared Helpers ───────────────────────────────────────────────────────────

    private RegisterRequestDto requestFor(String email) {
        return RegisterRequestDto.builder().email(email).build();
    }

    // Stub save() to return the passed identity and capture it for assertions
    private void stubSaveReturnsArgument() {
        when(identityRepository.save(any(Identity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Identity captureSavedIdentity() {
        ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
        verify(identityRepository).save(captor.capture());
        return captor.getValue();
    }

    /** Builds a minimal unverified identity with a mutable OTP list. */
    private Identity unverifiedIdentity() {
        Identity identity = new Identity();
        identity.setUserId("test-user-id");
        identity.setEmail("user@example.com");
        identity.setEmailVerified(false);
        identity.setRole(Role.USER);
        identity.setCreatedAt(LocalDateTime.now());
        identity.setUpdatedAt(LocalDateTime.now());
        identity.setOtpVerifications(new ArrayList<>());
        return identity;
    }

    /**
     * Builds an OtpVerification whose hash is produced from a known raw OTP string.
     * Using the real encoder here ensures BCrypt semantics are exercised in tests.
     */
    private OtpVerification otpFor(Identity identity, String rawOtp, boolean verified, LocalDateTime expiresAt) {
        OtpVerification otp = new OtpVerification();
        otp.setIdentity(identity);
        otp.setOtpHash(passwordEncoder.encode(rawOtp));
        otp.setVerified(verified);
        otp.setCreatedAt(LocalDateTime.now());
        otp.setExpiresAt(expiresAt);
        return otp;
    }

    private VerifyOtpRequestDto verifyRequest(String email, String otp) {
        return VerifyOtpRequestDto.builder().email(email).otp(otp).build();
    }

    private ResendOtpRequestDto resendRequest(String email) {
        return ResendOtpRequestDto.builder().email(email).build();
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // Registration Tests (Milestone 4)
    // ═════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Registration")
    class RegistrationTests {

        @Test
        @DisplayName("1. Successful registration returns a message response")
        void successfulRegistration_returnsMessageResponse() {
            when(identityRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            stubSaveReturnsArgument();

            MessageResponseDto response = authenticationService.register(requestFor("user@example.com"));

            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isNotBlank();
        }

        @Test
        @DisplayName("2. Email is normalised to lowercase before persistence")
        void register_normalisesEmailToLowercase() {
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
            stubSaveReturnsArgument();

            authenticationService.register(requestFor("USER@EXAMPLE.COM"));

            assertThat(captureSavedIdentity().getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("3. Surrounding whitespace is stripped from email before persistence")
        void register_trimsSurroundingWhitespace() {
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
            stubSaveReturnsArgument();

            authenticationService.register(requestFor("  user@example.com  "));

            assertThat(captureSavedIdentity().getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("4. Generated userId is present and unique across two registrations")
        void register_generatesUniqueUserId() {
            when(identityRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(identityRepository.save(any(Identity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            authenticationService.register(requestFor("a@example.com"));
            authenticationService.register(requestFor("b@example.com"));

            ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
            verify(identityRepository, times(2)).save(captor.capture());

            String userId1 = captor.getAllValues().get(0).getUserId();
            String userId2 = captor.getAllValues().get(1).getUserId();

            assertThat(userId1).isNotBlank();
            assertThat(userId2).isNotBlank();
            assertThat(userId1).isNotEqualTo(userId2);
        }

        @Test
        @DisplayName("5. New identity has emailVerified = false")
        void register_setsEmailVerifiedFalse() {
            when(identityRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            stubSaveReturnsArgument();

            authenticationService.register(requestFor("user@example.com"));

            assertThat(captureSavedIdentity().isEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("6. New identity is assigned Role.USER")
        void register_assignsDefaultRoleUser() {
            when(identityRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            stubSaveReturnsArgument();

            authenticationService.register(requestFor("user@example.com"));

            assertThat(captureSavedIdentity().getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("7. Password hash remains null after registration")
        void register_passwordHashRemainsNull() {
            when(identityRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            stubSaveReturnsArgument();

            authenticationService.register(requestFor("user@example.com"));

            assertThat(captureSavedIdentity().getPasswordHash()).isNull();
        }

        @Test
        @DisplayName("8. OTP verification record is created for the new identity")
        void register_createsOtpVerification() {
            when(identityRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            stubSaveReturnsArgument();

            authenticationService.register(requestFor("user@example.com"));

            assertThat(captureSavedIdentity().getOtpVerifications()).hasSize(1);
        }

        @Test
        @DisplayName("9. OTP is stored only as a BCrypt hash, not as plaintext")
        void register_storesOtpAsHashOnly() {
            when(identityRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            stubSaveReturnsArgument();

            authenticationService.register(requestFor("user@example.com"));

            OtpVerification otp = captureSavedIdentity().getOtpVerifications().get(0);

            assertThat(otp.getOtpHash()).isNotBlank();
            // BCrypt hashes always begin with $2 — confirms the value was encoded, not stored raw
            assertThat(otp.getOtpHash()).startsWith("$2");
            // A raw 6-digit OTP would be numeric only; the hash must not be purely numeric
            assertThat(otp.getOtpHash()).containsPattern("[^0-9]");
        }

        @Test
        @DisplayName("10. OTP is not marked as verified at creation")
        void register_otpIsNotVerified() {
            when(identityRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            stubSaveReturnsArgument();

            authenticationService.register(requestFor("user@example.com"));

            assertThat(captureSavedIdentity().getOtpVerifications().get(0).isVerified()).isFalse();
        }

        @Test
        @DisplayName("11. OTP expiry is approximately 10 minutes from creation time")
        void register_otpExpiryIsApproximatelyTenMinutes() {
            when(identityRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            stubSaveReturnsArgument();

            LocalDateTime before = LocalDateTime.now();
            authenticationService.register(requestFor("user@example.com"));
            LocalDateTime after = LocalDateTime.now();

            OtpVerification otp = captureSavedIdentity().getOtpVerifications().get(0);

            assertThat(otp.getExpiresAt()).isAfterOrEqualTo(before.plusMinutes(10));
            assertThat(otp.getExpiresAt()).isBeforeOrEqualTo(after.plusMinutes(10));
        }

        @Test
        @DisplayName("12. Registration fails when email already has an identity")
        void register_throwsException_whenEmailAlreadyExists() {
            Identity existing = new Identity();
            existing.setEmail("user@example.com");
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> authenticationService.register(requestFor("user@example.com")))
                    .isInstanceOf(IdentityAlreadyExistsException.class);
        }

        @Test
        @DisplayName("13. Duplicate registration does not create a second identity")
        void register_duplicateEmail_doesNotSaveNewIdentity() {
            Identity existing = new Identity();
            existing.setEmail("user@example.com");
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> authenticationService.register(requestFor("user@example.com")))
                    .isInstanceOf(IdentityAlreadyExistsException.class);

            verify(identityRepository, never()).save(any(Identity.class));
        }

        @Test
        @DisplayName("14. Duplicate registration does not create a new OTP")
        void register_duplicateEmail_doesNotCreateOtp() {
            Identity existing = new Identity();
            existing.setEmail("user@example.com");
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> authenticationService.register(requestFor("user@example.com")))
                    .isInstanceOf(IdentityAlreadyExistsException.class);

            // OTP is persisted only via CascadeType.ALL when identity is saved; no save = no OTP
            verify(identityRepository, never()).save(any());
        }

        @Test
        @DisplayName("15. Registration method carries the @Transactional annotation")
        void register_isMarkedTransactional() throws NoSuchMethodException {
            var method = AuthenticationServiceImpl.class.getMethod("register", RegisterRequestDto.class);
            assertThat(method.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class))
                    .isTrue();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // OTP Verification Tests (Milestone 5)
    // ═════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("OTP Verification")
    class OtpVerificationTests {

        private static final String KNOWN_OTP = "654321";

        @Test
        @DisplayName("1. Valid OTP verifies the identity successfully (returns message response)")
        void verifyOtp_validOtp_returnsMessageResponse() {
            Identity identity = unverifiedIdentity();
            OtpVerification otp = otpFor(identity, KNOWN_OTP, false, LocalDateTime.now().plusMinutes(10));

            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(otpVerificationRepository.findTopByIdentityAndVerifiedFalseOrderByCreatedAtDesc(identity))
                    .thenReturn(Optional.of(otp));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MessageResponseDto response = authenticationService.verifyOtp(verifyRequest("user@example.com", KNOWN_OTP));

            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isNotBlank();
        }

        @Test
        @DisplayName("2. Valid OTP sets identity.emailVerified = true")
        void verifyOtp_validOtp_setsEmailVerifiedTrue() {
            Identity identity = unverifiedIdentity();
            OtpVerification otp = otpFor(identity, KNOWN_OTP, false, LocalDateTime.now().plusMinutes(10));

            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(otpVerificationRepository.findTopByIdentityAndVerifiedFalseOrderByCreatedAtDesc(identity))
                    .thenReturn(Optional.of(otp));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authenticationService.verifyOtp(verifyRequest("user@example.com", KNOWN_OTP));

            assertThat(identity.isEmailVerified()).isTrue();
        }

        @Test
        @DisplayName("3. Valid OTP sets OtpVerification.verified = true")
        void verifyOtp_validOtp_setsOtpVerifiedTrue() {
            Identity identity = unverifiedIdentity();
            OtpVerification otp = otpFor(identity, KNOWN_OTP, false, LocalDateTime.now().plusMinutes(10));

            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(otpVerificationRepository.findTopByIdentityAndVerifiedFalseOrderByCreatedAtDesc(identity))
                    .thenReturn(Optional.of(otp));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authenticationService.verifyOtp(verifyRequest("user@example.com", KNOWN_OTP));

            assertThat(otp.isVerified()).isTrue();
        }

        @Test
        @DisplayName("4. Invalid OTP is rejected with OtpInvalidException")
        void verifyOtp_invalidOtp_throwsOtpInvalidException() {
            Identity identity = unverifiedIdentity();
            OtpVerification otp = otpFor(identity, KNOWN_OTP, false, LocalDateTime.now().plusMinutes(10));

            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(otpVerificationRepository.findTopByIdentityAndVerifiedFalseOrderByCreatedAtDesc(identity))
                    .thenReturn(Optional.of(otp));

            assertThatThrownBy(() -> authenticationService.verifyOtp(verifyRequest("user@example.com", "000000")))
                    .isInstanceOf(OtpInvalidException.class);
        }

        @Test
        @DisplayName("5. Expired OTP is rejected with OtpExpiredException")
        void verifyOtp_expiredOtp_throwsOtpExpiredException() {
            Identity identity = unverifiedIdentity();
            // expiresAt is in the past
            OtpVerification otp = otpFor(identity, KNOWN_OTP, false, LocalDateTime.now().minusMinutes(1));

            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(otpVerificationRepository.findTopByIdentityAndVerifiedFalseOrderByCreatedAtDesc(identity))
                    .thenReturn(Optional.of(otp));

            assertThatThrownBy(() -> authenticationService.verifyOtp(verifyRequest("user@example.com", KNOWN_OTP)))
                    .isInstanceOf(OtpExpiredException.class);
        }

        @Test
        @DisplayName("6. Already verified identity cannot be verified again")
        void verifyOtp_alreadyVerified_throwsIdentityAlreadyVerifiedException() {
            Identity identity = unverifiedIdentity();
            identity.setEmailVerified(true);

            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));

            assertThatThrownBy(() -> authenticationService.verifyOtp(verifyRequest("user@example.com", KNOWN_OTP)))
                    .isInstanceOf(IdentityAlreadyVerifiedException.class);
        }

        @Test
        @DisplayName("7. Unknown identity is rejected with IdentityNotFoundException")
        void verifyOtp_unknownIdentity_throwsIdentityNotFoundException() {
            when(identityRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.verifyOtp(verifyRequest("unknown@example.com", KNOWN_OTP)))
                    .isInstanceOf(IdentityNotFoundException.class);
        }

        @Test
        @DisplayName("8. Already-used OTP (verified=true) cannot be reused — no active OTP found")
        void verifyOtp_alreadyVerifiedOtp_throwsOtpNotFoundException() {
            Identity identity = unverifiedIdentity();
            // The repository query filters verified=false, so a used OTP is never returned
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(otpVerificationRepository.findTopByIdentityAndVerifiedFalseOrderByCreatedAtDesc(identity))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.verifyOtp(verifyRequest("user@example.com", KNOWN_OTP)))
                    .isInstanceOf(OtpNotFoundException.class);
        }

        @Test
        @DisplayName("9. Old OTP cannot verify after a newer OTP has been issued")
        void verifyOtp_oldOtpAfterResend_isRejected() {
            Identity identity = unverifiedIdentity();
            String oldOtp = "111111";
            String newOtp = "222222";

            // Repository only returns the latest — the new one — so the old hash is never checked
            OtpVerification latestOtp = otpFor(identity, newOtp, false, LocalDateTime.now().plusMinutes(10));

            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(otpVerificationRepository.findTopByIdentityAndVerifiedFalseOrderByCreatedAtDesc(identity))
                    .thenReturn(Optional.of(latestOtp));

            // Supplying the old OTP raw value must not match the new OTP's hash
            assertThatThrownBy(() -> authenticationService.verifyOtp(verifyRequest("user@example.com", oldOtp)))
                    .isInstanceOf(OtpInvalidException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // OTP Resend Tests (Milestone 5)
    // ═════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("OTP Resend")
    class OtpResendTests {

        @Test
        @DisplayName("10. Unknown identity is rejected with IdentityNotFoundException")
        void resendOtp_unknownIdentity_throwsIdentityNotFoundException() {
            when(identityRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.resendOtp(resendRequest("unknown@example.com")))
                    .isInstanceOf(IdentityNotFoundException.class);
        }

        @Test
        @DisplayName("11. Already verified identity cannot request another OTP")
        void resendOtp_alreadyVerified_throwsIdentityAlreadyVerifiedException() {
            Identity identity = unverifiedIdentity();
            identity.setEmailVerified(true);

            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));

            assertThatThrownBy(() -> authenticationService.resendOtp(resendRequest("user@example.com")))
                    .isInstanceOf(IdentityAlreadyVerifiedException.class);
        }

        @Test
        @DisplayName("12. Resend creates a new OTP record on the identity")
        void resendOtp_createsNewOtpRecord() {
            Identity identity = unverifiedIdentity();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authenticationService.resendOtp(resendRequest("user@example.com"));

            ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
            verify(identityRepository).save(captor.capture());
            assertThat(captor.getValue().getOtpVerifications()).hasSize(1);
        }

        @Test
        @DisplayName("13. New OTP is stored only as a BCrypt hash")
        void resendOtp_newOtpStoredAsBcryptHash() {
            Identity identity = unverifiedIdentity();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authenticationService.resendOtp(resendRequest("user@example.com"));

            ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
            verify(identityRepository).save(captor.capture());
            String hash = captor.getValue().getOtpVerifications().get(0).getOtpHash();

            assertThat(hash).startsWith("$2");
            assertThat(hash).containsPattern("[^0-9]");
        }

        @Test
        @DisplayName("14. New OTP record is unverified")
        void resendOtp_newOtpIsUnverified() {
            Identity identity = unverifiedIdentity();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authenticationService.resendOtp(resendRequest("user@example.com"));

            ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
            verify(identityRepository).save(captor.capture());
            assertThat(captor.getValue().getOtpVerifications().get(0).isVerified()).isFalse();
        }

        @Test
        @DisplayName("15. New OTP has approximately 10-minute expiry")
        void resendOtp_newOtpExpiryIsApproximatelyTenMinutes() {
            Identity identity = unverifiedIdentity();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LocalDateTime before = LocalDateTime.now();
            authenticationService.resendOtp(resendRequest("user@example.com"));
            LocalDateTime after = LocalDateTime.now();

            ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
            verify(identityRepository).save(captor.capture());
            LocalDateTime expiresAt = captor.getValue().getOtpVerifications().get(0).getExpiresAt();

            assertThat(expiresAt).isAfterOrEqualTo(before.plusMinutes(10));
            assertThat(expiresAt).isBeforeOrEqualTo(after.plusMinutes(10));
        }

        @Test
        @DisplayName("16. Previous OTP record remains in the database after resend")
        void resendOtp_previousOtpRecordIsPreserved() {
            Identity identity = unverifiedIdentity();
            // Simulate a pre-existing OTP from registration
            OtpVerification existing = otpFor(identity, "123456", false, LocalDateTime.now().plusMinutes(5));
            identity.getOtpVerifications().add(existing);

            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authenticationService.resendOtp(resendRequest("user@example.com"));

            // After resend the identity should have 2 OTPs: old + new
            ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
            verify(identityRepository).save(captor.capture());
            assertThat(captor.getValue().getOtpVerifications()).hasSize(2);
        }

        @Test
        @DisplayName("17. Previous OTP cannot be used after resend — repository returns only latest")
        void resendOtp_previousOtpCannotVerify_afterResend() {
            Identity identity = unverifiedIdentity();
            String oldRaw = "111111";
            String newRaw = "222222";

            OtpVerification oldOtp = otpFor(identity, oldRaw, false, LocalDateTime.now().plusMinutes(10));
            identity.getOtpVerifications().add(oldOtp);

            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            authenticationService.resendOtp(resendRequest("user@example.com"));

            // Simulate post-resend: repository returns only the new OTP (latest unverified)
            OtpVerification newOtpRecord = otpFor(identity, newRaw, false, LocalDateTime.now().plusMinutes(10));
            when(otpVerificationRepository.findTopByIdentityAndVerifiedFalseOrderByCreatedAtDesc(identity))
                    .thenReturn(Optional.of(newOtpRecord));

            // Supplying the old raw OTP must fail because the hash belongs to the new OTP
            assertThatThrownBy(() -> authenticationService.verifyOtp(verifyRequest("user@example.com", oldRaw)))
                    .isInstanceOf(OtpInvalidException.class);
        }

        @Test
        @DisplayName("18. New OTP can successfully verify the identity after resend")
        void resendOtp_newOtpCanVerifyIdentity() {
            Identity identity = unverifiedIdentity();
            String newRaw = "777777";

            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            authenticationService.resendOtp(resendRequest("user@example.com"));

            // Build an OTP record matching the new raw value to simulate what would be returned
            OtpVerification newOtpRecord = otpFor(identity, newRaw, false, LocalDateTime.now().plusMinutes(10));
            when(otpVerificationRepository.findTopByIdentityAndVerifiedFalseOrderByCreatedAtDesc(identity))
                    .thenReturn(Optional.of(newOtpRecord));

            MessageResponseDto response = authenticationService.verifyOtp(verifyRequest("user@example.com", newRaw));

            assertThat(response).isNotNull();
            assertThat(identity.isEmailVerified()).isTrue();
        }

        @Test
        @DisplayName("19. Raw OTP is never persisted — stored value is a BCrypt hash")
        void resendOtp_rawOtpIsNeverPersisted() {
            Identity identity = unverifiedIdentity();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authenticationService.resendOtp(resendRequest("user@example.com"));

            ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
            verify(identityRepository).save(captor.capture());
            String stored = captor.getValue().getOtpVerifications().get(0).getOtpHash();

            // A raw 6-digit OTP is purely numeric and 6 chars; the stored value must differ
            assertThat(stored).doesNotMatch("^\\d{6}$");
            assertThat(stored).startsWith("$2");
        }

        @Test
        @DisplayName("20. resendOtp method carries the @Transactional annotation")
        void resendOtp_isMarkedTransactional() throws NoSuchMethodException {
            var method = AuthenticationServiceImpl.class.getMethod("resendOtp", ResendOtpRequestDto.class);
            assertThat(method.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class))
                    .isTrue();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // Password Setup Tests (Milestone 6)
    // ═════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Password Setup")
    class SetPasswordTests {

        private Identity verifiedIdentity() {
            Identity identity = unverifiedIdentity();
            identity.setEmailVerified(true);
            return identity;
        }

        private SetPasswordRequestDto setPasswordRequest(String email, String password, String confirm) {
            return SetPasswordRequestDto.builder().email(email).password(password).confirmPassword(confirm).build();
        }

        @Test
        @DisplayName("1. Verified identity can set a password successfully")
        void setPassword_verifiedIdentity_succeeds() {
            Identity identity = verifiedIdentity();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MessageResponseDto response = authenticationService.setPassword(
                    setPasswordRequest("user@example.com", "Passw0rd!", "Passw0rd!"));

            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isNotBlank();
        }

        @Test
        @DisplayName("2. Password is stored as a BCrypt hash")
        void setPassword_passwordStoredAsBcryptHash() {
            Identity identity = verifiedIdentity();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authenticationService.setPassword(setPasswordRequest("user@example.com", "Passw0rd!", "Passw0rd!"));

            ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
            verify(identityRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).startsWith("$2");
        }

        @Test
        @DisplayName("3. Raw password is not stored")
        void setPassword_rawPasswordIsNotStored() {
            Identity identity = verifiedIdentity();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authenticationService.setPassword(setPasswordRequest("user@example.com", "Passw0rd!", "Passw0rd!"));

            ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
            verify(identityRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("Passw0rd!");
        }

        @Test
        @DisplayName("4. Password hash is not equal to the raw password")
        void setPassword_hashDoesNotEqualRawPassword() {
            Identity identity = verifiedIdentity();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authenticationService.setPassword(setPasswordRequest("user@example.com", "secret", "secret"));

            ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
            verify(identityRepository).save(captor.capture());
            // BCrypt output always differs from the input plaintext
            assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("secret");
        }

        @Test
        @DisplayName("5. BCrypt.matches(rawPassword, storedHash) succeeds after password set")
        void setPassword_bcryptMatchesSucceeds() {
            Identity identity = verifiedIdentity();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authenticationService.setPassword(setPasswordRequest("user@example.com", "MyP@ss1", "MyP@ss1"));

            ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
            verify(identityRepository).save(captor.capture());
            // Real BCrypt round-trip — not mocked
            assertThat(passwordEncoder.matches("MyP@ss1", captor.getValue().getPasswordHash())).isTrue();
        }

        @Test
        @DisplayName("6. Unverified identity cannot set a password")
        void setPassword_unverifiedIdentity_throwsEmailNotVerifiedException() {
            Identity identity = unverifiedIdentity(); // emailVerified = false
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));

            assertThatThrownBy(() -> authenticationService.setPassword(
                    setPasswordRequest("user@example.com", "Passw0rd!", "Passw0rd!")))
                    .isInstanceOf(EmailNotVerifiedException.class);
        }

        @Test
        @DisplayName("7. Non-existent identity is rejected with IdentityNotFoundException")
        void setPassword_unknownIdentity_throwsIdentityNotFoundException() {
            when(identityRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.setPassword(
                    setPasswordRequest("nobody@example.com", "Passw0rd!", "Passw0rd!")))
                    .isInstanceOf(IdentityNotFoundException.class);
        }

        @Test
        @DisplayName("8. Mismatched password and confirmPassword are rejected")
        void setPassword_passwordMismatch_throwsPasswordMismatchException() {
            Identity identity = verifiedIdentity();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));

            assertThatThrownBy(() -> authenticationService.setPassword(
                    setPasswordRequest("user@example.com", "Passw0rd!", "Different!")))
                    .isInstanceOf(PasswordMismatchException.class);
        }

        @Test
        @DisplayName("9. Mismatched passwords do not modify the identity")
        void setPassword_passwordMismatch_doesNotSaveIdentity() {
            Identity identity = verifiedIdentity();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));

            assertThatThrownBy(() -> authenticationService.setPassword(
                    setPasswordRequest("user@example.com", "Passw0rd!", "Different!")))
                    .isInstanceOf(PasswordMismatchException.class);

            verify(identityRepository, never()).save(any());
            assertThat(identity.getPasswordHash()).isNull();
        }

        @Test
        @DisplayName("10. Already-set password cannot be overwritten")
        void setPassword_passwordAlreadySet_throwsPasswordAlreadySetException() {
            Identity identity = verifiedIdentity();
            // Simulate a pre-existing password hash
            identity.setPasswordHash(passwordEncoder.encode("OldPass!"));

            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));

            assertThatThrownBy(() -> authenticationService.setPassword(
                    setPasswordRequest("user@example.com", "NewPass!", "NewPass!")))
                    .isInstanceOf(PasswordAlreadySetException.class);
        }

        @Test
        @DisplayName("11. Successful password setup updates updatedAt")
        void setPassword_success_updatesUpdatedAt() {
            Identity identity = verifiedIdentity();
            LocalDateTime originalUpdatedAt = identity.getUpdatedAt();

            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LocalDateTime before = LocalDateTime.now();
            authenticationService.setPassword(setPasswordRequest("user@example.com", "Passw0rd!", "Passw0rd!"));

            assertThat(identity.getUpdatedAt()).isAfterOrEqualTo(before);
            assertThat(identity.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        }

        @Test
        @DisplayName("12. Successful password setup does not change createdAt")
        void setPassword_success_doesNotChangeCreatedAt() {
            Identity identity = verifiedIdentity();
            LocalDateTime originalCreatedAt = identity.getCreatedAt();

            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authenticationService.setPassword(setPasswordRequest("user@example.com", "Passw0rd!", "Passw0rd!"));

            assertThat(identity.getCreatedAt()).isEqualTo(originalCreatedAt);
        }

        @Test
        @DisplayName("13. Password setup is transactional")
        void setPassword_isMarkedTransactional() throws NoSuchMethodException {
            var method = AuthenticationServiceImpl.class.getMethod("setPassword", SetPasswordRequestDto.class);
            assertThat(method.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class))
                    .isTrue();
        }

        @Test
        @DisplayName("14. Password is never returned in MessageResponseDto")
        void setPassword_responseDoesNotContainPassword() {
            Identity identity = verifiedIdentity();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MessageResponseDto response = authenticationService.setPassword(
                    setPasswordRequest("user@example.com", "Passw0rd!", "Passw0rd!"));

            // MessageResponseDto only carries a single message field — no credential leakage possible
            assertThat(response.getMessage()).doesNotContain("Passw0rd!");
            assertThat(response.getMessage()).doesNotContain("$2");
        }

        @Test
        @DisplayName("15. confirmPassword is never persisted")
        void setPassword_confirmPasswordIsNeverPersisted() {
            Identity identity = verifiedIdentity();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authenticationService.setPassword(setPasswordRequest("user@example.com", "Passw0rd!", "Passw0rd!"));

            ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
            verify(identityRepository).save(captor.capture());
            // The entity has only passwordHash; there is no confirmPassword field
            assertThat(captor.getValue().getPasswordHash()).isNotNull();
            assertThat(captor.getValue().getPasswordHash()).startsWith("$2");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // Login / Credential Authentication Tests (Milestone 7)
    // ═════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Login / Credential Authentication")
    class LoginTests {

        private Identity readyToLoginIdentity(String rawPassword) {
            Identity identity = unverifiedIdentity();
            identity.setEmailVerified(true);
            identity.setPasswordHash(passwordEncoder.encode(rawPassword));
            return identity;
        }

        private LoginRequestDto loginRequest(String email, String password) {
            return LoginRequestDto.builder().email(email).password(password).build();
        }

        private void mockSuccessfulAuthentication(Identity identity) {
            org.springframework.security.core.Authentication auth = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
            com.rishabh.microservices.auth.security.CustomUserDetails userDetails =
                    new com.rishabh.microservices.auth.security.CustomUserDetails(identity);
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtService.generateToken(any())).thenReturn("mock.jwt.token");
        }

        @Test
        @DisplayName("1. Verified user with correct password authenticates successfully and receives JWT")
        void login_verifiedUserWithCorrectPassword_authenticatesSuccessfully() {
            Identity identity = readyToLoginIdentity("SecretP@ss1");
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            mockSuccessfulAuthentication(identity);

            AuthResponseDto response = authenticationService.login(loginRequest("user@example.com", "SecretP@ss1"));

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("mock.jwt.token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getUserId()).isEqualTo("test-user-id");
            assertThat(response.getEmail()).isEqualTo("user@example.com");
            assertThat(response.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("2. Incorrect password is rejected with InvalidCredentialsException")
        void login_incorrectPassword_throwsInvalidCredentialsException() {
            Identity identity = readyToLoginIdentity("CorrectP@ss");
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authenticationService.login(loginRequest("user@example.com", "WrongP@ss")))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("3. Unknown email is rejected with IdentityNotFoundException")
        void login_unknownEmail_throwsIdentityNotFoundException() {
            when(identityRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.login(loginRequest("nobody@example.com", "SecretP@ss1")))
                    .isInstanceOf(IdentityNotFoundException.class);
        }

        @Test
        @DisplayName("4. Unverified email cannot log in — throws EmailNotVerifiedException")
        void login_unverifiedEmail_throwsEmailNotVerifiedException() {
            Identity identity = readyToLoginIdentity("SecretP@ss1");
            identity.setEmailVerified(false);
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));

            assertThatThrownBy(() -> authenticationService.login(loginRequest("user@example.com", "SecretP@ss1")))
                    .isInstanceOf(EmailNotVerifiedException.class);
        }

        @Test
        @DisplayName("5. User without passwordHash cannot log in — throws PasswordNotConfiguredException")
        void login_userWithoutPasswordHash_throwsPasswordNotConfiguredException() {
            Identity identity = unverifiedIdentity();
            identity.setEmailVerified(true);
            identity.setPasswordHash(null);
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));

            assertThatThrownBy(() -> authenticationService.login(loginRequest("user@example.com", "SecretP@ss1")))
                    .isInstanceOf(PasswordNotConfiguredException.class);
        }

        @Test
        @DisplayName("6. Email normalization works during login")
        void login_normalizesEmailBeforeLookup() {
            Identity identity = readyToLoginIdentity("SecretP@ss1");
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            mockSuccessfulAuthentication(identity);

            AuthResponseDto response = authenticationService.login(loginRequest(" USER@EXAMPLE.COM ", "SecretP@ss1"));

            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo("user@example.com");
            verify(identityRepository).findByEmail("user@example.com");
        }

        @Test
        @DisplayName("7. Successful authentication identifies the correct user")
        void login_successfulAuthentication_identifiesCorrectUser() {
            Identity identity = readyToLoginIdentity("SecretP@ss1");
            identity.setUserId("unique-user-id-999");
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            mockSuccessfulAuthentication(identity);

            AuthResponseDto response = authenticationService.login(loginRequest("user@example.com", "SecretP@ss1"));

            assertThat(response.getUserId()).isEqualTo("unique-user-id-999");
        }

        @Test
        @DisplayName("8. Correct Role.USER is preserved")
        void login_preservesCorrectRole() {
            Identity identity = readyToLoginIdentity("SecretP@ss1");
            identity.setRole(Role.USER);
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            mockSuccessfulAuthentication(identity);

            AuthResponseDto response = authenticationService.login(loginRequest("user@example.com", "SecretP@ss1"));

            assertThat(response.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("9. Login does not modify passwordHash")
        void login_doesNotModifyPasswordHash() {
            Identity identity = readyToLoginIdentity("SecretP@ss1");
            String originalHash = identity.getPasswordHash();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            mockSuccessfulAuthentication(identity);

            authenticationService.login(loginRequest("user@example.com", "SecretP@ss1"));

            assertThat(identity.getPasswordHash()).isEqualTo(originalHash);
            verify(identityRepository, never()).save(any());
        }

        @Test
        @DisplayName("10. Login does not modify emailVerified")
        void login_doesNotModifyEmailVerified() {
            Identity identity = readyToLoginIdentity("SecretP@ss1");
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            mockSuccessfulAuthentication(identity);

            authenticationService.login(loginRequest("user@example.com", "SecretP@ss1"));

            assertThat(identity.isEmailVerified()).isTrue();
            verify(identityRepository, never()).save(any());
        }

        @Test
        @DisplayName("11. Login does not generate or persist OTP")
        void login_doesNotGenerateOrPersistOtp() {
            Identity identity = readyToLoginIdentity("SecretP@ss1");
            int initialOtpCount = identity.getOtpVerifications().size();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            mockSuccessfulAuthentication(identity);

            authenticationService.login(loginRequest("user@example.com", "SecretP@ss1"));

            assertThat(identity.getOtpVerifications()).hasSize(initialOtpCount);
            verify(otpVerificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("12. Login generates a JWT token and Bearer tokenType")
        void login_generatesJwtTokenAndBearerType() {
            Identity identity = readyToLoginIdentity("SecretP@ss1");
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            mockSuccessfulAuthentication(identity);

            AuthResponseDto response = authenticationService.login(loginRequest("user@example.com", "SecretP@ss1"));

            assertThat(response.getToken()).isEqualTo("mock.jwt.token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("13. Raw password is never returned")
        void login_rawPasswordIsNeverReturned() {
            Identity identity = readyToLoginIdentity("SecretP@ss1");
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            mockSuccessfulAuthentication(identity);

            AuthResponseDto response = authenticationService.login(loginRequest("user@example.com", "SecretP@ss1"));

            assertThat(response.getUserId()).doesNotContain("SecretP@ss1");
            assertThat(response.getEmail()).doesNotContain("SecretP@ss1");
        }

        @Test
        @DisplayName("14. passwordHash is never returned")
        void login_passwordHashIsNeverReturned() {
            Identity identity = readyToLoginIdentity("SecretP@ss1");
            String hash = identity.getPasswordHash();
            when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));
            mockSuccessfulAuthentication(identity);

            AuthResponseDto response = authenticationService.login(loginRequest("user@example.com", "SecretP@ss1"));

            assertThat(response.getUserId()).doesNotContain(hash);
            assertThat(response.getEmail()).doesNotContain(hash);
        }
    }
}

