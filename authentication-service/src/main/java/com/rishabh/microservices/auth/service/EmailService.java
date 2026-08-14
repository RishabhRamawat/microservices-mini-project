package com.rishabh.microservices.auth.service;

// Service interface for transactional email delivery.
public interface EmailService {

    void sendOtpEmail(String recipientEmail, String rawOtp);
}
