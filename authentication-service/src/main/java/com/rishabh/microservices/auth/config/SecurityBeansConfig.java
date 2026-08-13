package com.rishabh.microservices.auth.config;

import com.rishabh.microservices.auth.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;

@Configuration
public class SecurityBeansConfig {

    // Shared BCrypt encoder used for OTP hashing, password hashing, and credential verification.
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Single SecureRandom instance; thread-safe and shared across the application.
    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }

    // DaoAuthenticationProvider: delegates user lookup to CustomUserDetailsService and BCrypt verification.
    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
                                                            BCryptPasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    // Explicit ProviderManager wrapping DaoAuthenticationProvider; avoids Spring Security circular-proxy issues.
    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }
}
