package com.rishabh.microservices.auth.security;

import com.rishabh.microservices.auth.entity.Identity;
import com.rishabh.microservices.auth.enums.Role;
import com.rishabh.microservices.auth.repository.IdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock
    private IdentityRepository identityRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(identityRepository);
    }

    @Test
    @DisplayName("1. loadUserByUsername returns valid UserDetails for existing identity")
    void loadUserByUsername_returnsUserDetails() {
        Identity identity = new Identity();
        identity.setUserId("user-123");
        identity.setEmail("user@example.com");
        identity.setPasswordHash("$2a$10$hash");
        identity.setEmailVerified(true);
        identity.setRole(Role.USER);

        when(identityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(identity));

        UserDetails userDetails = userDetailsService.loadUserByUsername("USER@EXAMPLE.COM ");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("user@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("2. loadUserByUsername throws UsernameNotFoundException when user is absent")
    void loadUserByUsername_throwsUsernameNotFoundException() {
        when(identityRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
