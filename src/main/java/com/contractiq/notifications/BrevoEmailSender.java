package com.contractiq.notifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class BrevoEmailSender {
    private final RestClient restClient;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public BrevoEmailSender(@Value("${app.email.brevo.api-key:}") String apiKey,
                            @Value("${app.email.brevo.sender-email:}") String senderEmail,
                            @Value("${app.email.brevo.sender-name:ContractIQ}") String senderName) {
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(java.time.Duration.ofSeconds(5));
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(15));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    public void validateConfiguration() {
        if (apiKey.isBlank() || senderEmail.isBlank())
            throw new IllegalStateException("BREVO_API_KEY and BREVO_SENDER_EMAIL are required when Brevo email is enabled");
    }

    public void send(String recipient, String subject, String htmlContent) {
        validateConfiguration();
        restClient.post()
                .uri("https://api.brevo.com/v3/smtp/email")
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BrevoEmail(new Sender(senderName, senderEmail), List.of(new Recipient(recipient)),
                        subject, htmlContent))
                .retrieve()
                .toBodilessEntity();
    }

    private record Sender(String name, String email) {}
    private record Recipient(String email) {}
    private record BrevoEmail(Sender sender, List<Recipient> to, String subject, String htmlContent) {}
}
