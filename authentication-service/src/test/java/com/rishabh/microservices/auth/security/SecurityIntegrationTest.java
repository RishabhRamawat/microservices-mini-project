package com.rishabh.microservices.auth.security;

import com.rishabh.microservices.auth.entity.Identity;
import com.rishabh.microservices.auth.enums.Role;
import com.rishabh.microservices.auth.repository.IdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@DisplayName("Spring Security & JWT Integration Tests")
class SecurityIntegrationTest {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private MockMvc mockMvc;

    private Identity verifiedIdentity;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        verifiedIdentity = new Identity();
        verifiedIdentity.setUserId("user-sec-123");
        verifiedIdentity.setEmail("secuser@example.com");
        verifiedIdentity.setPasswordHash(passwordEncoder.encode("ValidPass1!"));
        verifiedIdentity.setEmailVerified(true);
        verifiedIdentity.setRole(Role.USER);
        verifiedIdentity.setCreatedAt(LocalDateTime.now());
        verifiedIdentity.setUpdatedAt(LocalDateTime.now());

        identityRepository.save(verifiedIdentity);
    }

    @Test
    @DisplayName("14. AuthenticationManager authenticates correct credentials")
    void authenticationManager_correctCredentials_authenticates() {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("secuser@example.com", "ValidPass1!"));

        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isInstanceOf(CustomUserDetails.class);
    }

    @Test
    @DisplayName("15. Incorrect password is rejected with BadCredentialsException")
    void authenticationManager_incorrectPassword_throwsBadCredentialsException() {
        assertThatThrownBy(() -> authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("secuser@example.com", "WrongPass")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("16. Unverified identity is disabled in UserDetails")
    void unverifiedIdentity_hasDisabledUserDetails() {
        Identity unverified = new Identity();
        unverified.setUserId("unverified-id");
        unverified.setEmail("unverified@example.com");
        unverified.setPasswordHash(passwordEncoder.encode("Pass123!"));
        unverified.setEmailVerified(false);
        unverified.setRole(Role.USER);
        unverified.setCreatedAt(LocalDateTime.now());
        unverified.setUpdatedAt(LocalDateTime.now());

        identityRepository.save(unverified);

        UserDetails details = userDetailsService.loadUserByUsername("unverified@example.com");
        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("17. Identity without passwordHash returns null password in UserDetails")
    void identityWithoutPasswordHash_returnsNullPassword() {
        Identity noPassword = new Identity();
        noPassword.setUserId("nopass-id");
        noPassword.setEmail("nopassword@example.com");
        noPassword.setPasswordHash(null);
        noPassword.setEmailVerified(true);
        noPassword.setRole(Role.USER);
        noPassword.setCreatedAt(LocalDateTime.now());
        noPassword.setUpdatedAt(LocalDateTime.now());

        identityRepository.save(noPassword);

        UserDetails details = userDetailsService.loadUserByUsername("nopassword@example.com");
        assertThat(details.getPassword()).isNull();
    }

    @Test
    @DisplayName("18. CustomUserDetailsService loads the correct identity")
    void customUserDetailsService_loadsCorrectIdentity() {
        UserDetails details = userDetailsService.loadUserByUsername("secuser@example.com");

        assertThat(details).isNotNull();
        assertThat(details.getUsername()).isEqualTo("secuser@example.com");
        assertThat(((CustomUserDetails) details).getUserId()).isEqualTo("user-sec-123");
    }

    @Test
    @DisplayName("19. User role becomes the correct GrantedAuthority (ROLE_USER)")
    void userRole_becomesGrantedAuthority() {
        UserDetails details = userDetailsService.loadUserByUsername("secuser@example.com");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("20. JwtAuthenticationFilter authenticates a valid Bearer token")
    void jwtAuthenticationFilter_validToken_populatesSecurityContext() throws ServletException, IOException {
        CustomUserDetails userDetails = new CustomUserDetails(verifiedIdentity);
        String token = jwtService.generateToken(userDetails);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("secuser@example.com");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("21. JwtAuthenticationFilter rejects an invalid token")
    void jwtAuthenticationFilter_invalidToken_doesNotPopulateSecurityContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
    @Test
    @DisplayName("27. JWT for a non-existent user does not populate SecurityContext")
    void jwtAuthenticationFilter_nonExistentUser_doesNotPopulateSecurityContext()
            throws ServletException, IOException {

        Identity deletedIdentity = new Identity();
        deletedIdentity.setUserId("deleted-user-123");
        deletedIdentity.setEmail("deleted@example.com");
        deletedIdentity.setPasswordHash(passwordEncoder.encode("Password123!"));
        deletedIdentity.setEmailVerified(true);
        deletedIdentity.setRole(Role.USER);
        deletedIdentity.setCreatedAt(LocalDateTime.now());
        deletedIdentity.setUpdatedAt(LocalDateTime.now());

        CustomUserDetails deletedUserDetails = new CustomUserDetails(deletedIdentity);

        String token = jwtService.generateToken(deletedUserDetails);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("22. Missing token does not authenticate a request")
    void jwtAuthenticationFilter_missingToken_doesNotPopulateSecurityContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("23. SecurityContext contains the authenticated principal after valid JWT processing")
    void securityContext_containsAuthenticatedPrincipal() throws ServletException, IOException {
        CustomUserDetails userDetails = new CustomUserDetails(verifiedIdentity);
        String token = jwtService.generateToken(userDetails);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getPrincipal()).isInstanceOf(CustomUserDetails.class);
        assertThat(((CustomUserDetails) auth.getPrincipal()).getUserId()).isEqualTo("user-sec-123");
    }

    @Test
    @DisplayName("24. SecurityContext is not populated for invalid JWT")
    void securityContext_notPopulatedForInvalidJwt() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tamperedToken123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("25. Security configuration is stateless — no HTTP session created")
    void security_isStateless() throws Exception {
        // Protected path (not public) without JWT should return 401
        mockMvc.perform(post("/protected-endpoint-test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("26. Public authentication endpoints are accessible without JWT")
    void publicEndpoints_accessibleWithoutJwt() throws Exception {
        // Public endpoint returns 400 Bad Request (missing body) rather than 401 Unauthorized because security permits it through
        mockMvc.perform(post("/auth/login"))
                .andExpect(status().isBadRequest());
    }


}
