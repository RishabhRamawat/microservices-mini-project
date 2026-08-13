package com.rishabh.microservices.auth.security;

import com.rishabh.microservices.auth.config.JwtProperties;
import com.rishabh.microservices.auth.entity.Identity;
import com.rishabh.microservices.auth.enums.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    // Test-only 256+ bit Base64 secret key
    private static final String TEST_SECRET = "dGhpcy1pcy1hLXNlY3VyZS10ZXN0LWp3dC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW1pbGVzdG9uZS04";
    private static final long EXPIRATION_MS = 900_000; // 15 minutes

    private JwtService jwtService;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setExpirationMs(EXPIRATION_MS);

        jwtService = new JwtService(jwtProperties);

        Identity identity = new Identity();
        identity.setUserId("user-uuid-12345");
        identity.setEmail("user@example.com");
        identity.setPasswordHash("$2a$10$hashedPasswordHere");
        identity.setEmailVerified(true);
        identity.setRole(Role.USER);

        userDetails = new CustomUserDetails(identity);
    }

    @Test
    @DisplayName("1. JWT is generated successfully and is not blank")
    void generateToken_returnsNonBlankToken() {
        String token = jwtService.generateToken(userDetails);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("2. JWT contains userId claim")
    void generateToken_containsUserIdClaim() {
        String token = jwtService.generateToken(userDetails);
        String userId = jwtService.extractClaim(token, claims -> claims.get("userId", String.class));
        assertThat(userId).isEqualTo("user-uuid-12345");
    }

    @Test
    @DisplayName("3. JWT contains email (as subject)")
    void generateToken_containsEmailSubject() {
        String token = jwtService.generateToken(userDetails);
        String email = jwtService.extractUsername(token);
        assertThat(email).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("4. JWT contains role claim")
    void generateToken_containsRoleClaim() {
        String token = jwtService.generateToken(userDetails);
        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        assertThat(role).isEqualTo("USER");
    }

    @Test
    @DisplayName("5. JWT contains issuedAt date")
    void generateToken_containsIssuedAt() {
        String token = jwtService.generateToken(userDetails);
        Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);
        assertThat(issuedAt).isNotNull();
        assertThat(issuedAt).isBeforeOrEqualTo(new Date());
    }

    @Test
    @DisplayName("6. JWT contains expiration date matching configured window")
    void generateToken_containsExpiration() {
        String token = jwtService.generateToken(userDetails);
        Date expiration = jwtService.extractClaim(token, Claims::getExpiration);
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("7. JWT does not contain raw password")
    void generateToken_doesNotContainRawPassword() {
        String token = jwtService.generateToken(userDetails);
        assertThat(token).doesNotContain("rawPassword");
    }

    @Test
    @DisplayName("8. JWT does not contain passwordHash")
    void generateToken_doesNotContainPasswordHash() {
        String token = jwtService.generateToken(userDetails);
        assertThat(token).doesNotContain("$2a$10$hashedPasswordHere");
    }

    @Test
    @DisplayName("9. JWT does not contain OTP data")
    void generateToken_doesNotContainOtpData() {
        String token = jwtService.generateToken(userDetails);
        String jsonPayload = jwtService.extractClaim(token, Claims::toString);
        assertThat(jsonPayload).doesNotContain("otp");
    }

    @Test
    @DisplayName("10. Valid JWT validates successfully against UserDetails")
    void validateToken_validToken_returnsTrue() {
        String token = jwtService.generateToken(userDetails);
        boolean isValid = jwtService.validateToken(token, userDetails);
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("11. Tampered JWT is rejected")
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtService.generateToken(userDetails);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";
        boolean isValid = jwtService.validateToken(tamperedToken);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("12. Expired JWT is rejected")
    void validateToken_expiredToken_returnsFalse() {
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret(TEST_SECRET);
        expiredProps.setExpirationMs(-1000); // Expiration in the past

        JwtService expiredJwtService = new JwtService(expiredProps);
        String expiredToken = expiredJwtService.generateToken(userDetails);

        boolean isValid = jwtService.validateToken(expiredToken);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("13. Invalid signature is rejected")
    void validateToken_differentSecret_returnsFalse() {
        String token = jwtService.generateToken(userDetails);

        String otherSecret = "YW5vdGhlci1zZWN1cmUtdGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItdW5pdC10ZXN0cw==";
        JwtProperties otherProps = new JwtProperties();
        otherProps.setSecret(otherSecret);
        otherProps.setExpirationMs(EXPIRATION_MS);

        JwtService wrongSecretService = new JwtService(otherProps);
        boolean isValid = wrongSecretService.validateToken(token);
        assertThat(isValid).isFalse();
    }
}
