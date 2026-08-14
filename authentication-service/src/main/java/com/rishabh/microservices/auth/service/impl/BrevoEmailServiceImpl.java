package com.rishabh.microservices.auth.service.impl;

import com.rishabh.microservices.auth.exception.EmailSendException;
import com.rishabh.microservices.auth.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

// Delivers transactional emails via Brevo's v3 REST API.
@Service
public class BrevoEmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailServiceImpl.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestClient restClient;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public BrevoEmailServiceImpl(RestClient.Builder restClientBuilder,
                                @Value("${brevo.api-key:}") String apiKey,
                                @Value("${brevo.sender-email:}") String senderEmail,
                                @Value("${brevo.sender-name:Rishabh}") String senderName) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    @Override
    public void sendOtpEmail(String recipientEmail, String rawOtp) {
        // Prevent silent failures when the API key or sender email is missing from environment
        if (apiKey == null || apiKey.trim().isEmpty() || senderEmail == null || senderEmail.trim().isEmpty()) {
            log.error("Brevo API key or sender email is missing; email delivery aborted.");
            throw new EmailSendException("Email service configuration error.");
        }


        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(Map.of("email", recipientEmail)),
                "subject", "Verify your email",
                "htmlContent", "<html><body><p>Your verification OTP is <strong>" + rawOtp + "</strong>.</p>" +
                        "<p>This OTP is for email verification and expires in 10 minutes.</p></body></html>"
        );

        try {
            restClient.post()
                    .uri(BREVO_API_URL)
                    .header("api-key", apiKey)
                    .header("accept", MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Log diagnostic details without exposing API keys or raw OTPs
            log.error("Failed to send OTP email via Brevo API: {}", e.getMessage());
            throw new EmailSendException("Failed to send verification email. Please try again later.");
        }
    }
}
