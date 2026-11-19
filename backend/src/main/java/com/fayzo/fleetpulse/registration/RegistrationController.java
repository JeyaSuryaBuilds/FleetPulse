package com.fayzo.fleetpulse.registration;

import com.fayzo.fleetpulse.registration.RegistrationDtos.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/registration")
public class RegistrationController {

    private final RegistrationService service;

    public RegistrationController(RegistrationService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public StartResponse start(
            @Valid @RequestBody StartRequest r,
            HttpServletRequest h
    ) {
        return service.start(
                r,
                h.getRemoteAddr(),
                h.getHeader("User-Agent")
        );
    }

    @PostMapping("/verify-email")
    public VerifyResponse verifyEmail(
            @Valid @RequestBody VerifyRequest r,
            HttpServletRequest h
    ) {
        return service.verify(
                "EMAIL",
                r,
                h.getRemoteAddr(),
                h.getHeader("User-Agent")
        );
    }

    @PostMapping("/verify-mobile")
    public VerifyResponse verifyMobile(
            @Valid @RequestBody VerifyRequest r,
            HttpServletRequest h
    ) {
        return service.verify(
                "MOBILE",
                r,
                h.getRemoteAddr(),
                h.getHeader("User-Agent")
        );
    }

    @PostMapping("/resend")
    public StartResponse resend(
            @Valid @RequestBody ResendRequest r
    ) {
        return service.resend(r);
    }
}