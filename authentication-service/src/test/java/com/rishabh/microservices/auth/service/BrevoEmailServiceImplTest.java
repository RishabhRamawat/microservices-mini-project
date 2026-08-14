package com.rishabh.microservices.auth.service;

import com.rishabh.microservices.auth.exception.EmailSendException;
import com.rishabh.microservices.auth.service.impl.BrevoEmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("BrevoEmailServiceImpl Unit Tests")
class BrevoEmailServiceImplTest {

    private MockRestServiceServer mockServer;
    private BrevoEmailServiceImpl brevoEmailService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        brevoEmailService = new BrevoEmailServiceImpl(builder, "test-brevo-api-key", "sender@example.com", "Rishabh");
    }

    @Test
    @DisplayName("1. sendOtpEmail issues POST request to Brevo REST API with correct headers and payload")
    void sendOtpEmail_success() {
        mockServer.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-brevo-api-key"))
                .andExpect(header("accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("user@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("123456")))
                .andRespond(withSuccess());

        brevoEmailService.sendOtpEmail("user@example.com", "123456");
        mockServer.verify();
    }

    @Test
    @DisplayName("2. Missing API key throws EmailSendException before calling REST API")
    void sendOtpEmail_missingApiKey_throwsEmailSendException() {
        BrevoEmailServiceImpl serviceWithoutKey = new BrevoEmailServiceImpl(
                RestClient.builder(), "", "sender@example.com", "Rishabh");

        assertThatThrownBy(() -> serviceWithoutKey.sendOtpEmail("user@example.com", "123456"))
                .isInstanceOf(EmailSendException.class)
                .hasMessageContaining("Email service configuration error.");
    }

    @Test
    @DisplayName("3. HTTP error from Brevo API throws EmailSendException")
    void sendOtpEmail_httpError_throwsEmailSendException() {
        mockServer.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> brevoEmailService.sendOtpEmail("user@example.com", "123456"))
                .isInstanceOf(EmailSendException.class)
                .hasMessageContaining("Failed to send verification email. Please try again later.");
    }

    @Test
    @DisplayName("4. Missing sender email throws EmailSendException before calling REST API")
    void sendOtpEmail_missingSenderEmail_throwsEmailSendException() {
        BrevoEmailServiceImpl serviceWithoutSender = new BrevoEmailServiceImpl(
                RestClient.builder(), "test-brevo-api-key", "", "Rishabh");

        assertThatThrownBy(() -> serviceWithoutSender.sendOtpEmail("user@example.com", "123456"))
                .isInstanceOf(EmailSendException.class)
                .hasMessageContaining("Email service configuration error.");
    }
}

