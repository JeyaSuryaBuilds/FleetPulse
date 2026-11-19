package com.fayzo.fleetpulse.notify;

import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Base64;

/**
 * Delivers OTP codes through real providers only (SMTP and Twilio). When a channel is not configured the
 * call fails with 503 rather than pretending a code was sent. Codes are never logged.
 */
@Service
public class OtpDelivery {
    private static final Logger log = LoggerFactory.getLogger(OtpDelivery.class);
    private final AppProperties props;
    private final ObjectProvider<JavaMailSender> mail;

    public OtpDelivery(AppProperties props, ObjectProvider<JavaMailSender> mail) {
        this.props = props;
        this.mail = mail;
    }

    public void sendEmail(String to, String code) {
        if (!props.email().enabled() || mail.getIfAvailable() == null)
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_NOT_CONFIGURED", "Email delivery is not configured on this server");
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(props.email().from());
        m.setTo(to);
        m.setSubject("Your FleetPulse verification code");
        m.setText("Your FleetPulse email verification code is " + code + ". It expires in " + props.otp().ttlMinutes()
                + " minutes. If you did not request this, ignore this email.");
        try { mail.getObject().send(m); }
        catch (Exception e) {
            log.warn("Email OTP delivery failed: {}", e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "EMAIL_SEND_FAILED", "Could not send the email code");
        }
    }

    public void sendSms(String toE164, String code) {
        var s = props.sms();
        if (!s.enabled() || blank(s.twilioAccountSid()) || blank(s.twilioAuthToken()) || blank(s.twilioFrom()))
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_NOT_CONFIGURED", "SMS delivery is not configured on this server");
        var form = new LinkedMultiValueMap<String, String>();
        form.add("To", toE164);
        form.add("From", s.twilioFrom());
        form.add("Body", "Your FleetPulse verification code is " + code + ". Valid for " + props.otp().ttlMinutes() + " minutes.");
        String basic = Base64.getEncoder().encodeToString((s.twilioAccountSid() + ":" + s.twilioAuthToken()).getBytes());
        try {
            RestClient.create().post()
                    .uri("https://api.twilio.com/2010-04-01/Accounts/" + s.twilioAccountSid() + "/Messages.json")
                    .header("Authorization", "Basic " + basic)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("SMS OTP delivery failed: {}", e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "SMS_SEND_FAILED", "Could not send the SMS code");
        }
    }

    private static boolean blank(String v) { return v == null || v.isBlank(); }
}
