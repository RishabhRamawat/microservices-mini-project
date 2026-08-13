package com.rishabh.microservices.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Strongly-typed binding for jwt.* properties; avoids @Value scattering across security classes.
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    // Base64-encoded HMAC-SHA256 signing secret; must be at least 256 bits (32 bytes decoded).
    private String secret;

    // Token validity window in milliseconds; defaults to 900 000 ms (15 minutes).
    private long expirationMs = 900_000;
}
