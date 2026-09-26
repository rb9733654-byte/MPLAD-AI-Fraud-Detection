package com.mplad.fraud_detection.controller;

import com.mplad.fraud_detection.dto.PublicAuthRequest;
import com.mplad.fraud_detection.dto.DemoOtpResponse;
import com.mplad.fraud_detection.dto.PublicUserResponse;
import com.mplad.fraud_detection.entity.PublicUser;
import com.mplad.fraud_detection.service.PublicAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/public/auth")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:5502", "http://127.0.0.1:5502"}, allowCredentials = "true")
public class PublicAuthController {
    private final PublicAuthService auth;
    public PublicAuthController(PublicAuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    public ResponseEntity<DemoOtpResponse> register(@RequestBody PublicAuthRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(request.email(), request.password(), request.confirmPassword()));
    }
    @PostMapping("/verify-otp")
    public Map<String, String> verify(@RequestBody PublicAuthRequest request) {
        auth.verify(request.email(), request.otp()); return Map.of("message", "Account verified. You can now log in.");
    }
    @PostMapping("/resend-otp")
    public DemoOtpResponse resend(@RequestBody PublicAuthRequest request) { return auth.resend(request.email()); }
    @PostMapping("/login")
    public PublicUserResponse login(@RequestBody PublicAuthRequest request, HttpServletRequest httpRequest) {
        PublicUser user = auth.authenticate(request.email(), request.password());
        return auth.startSession(user, httpRequest);
    }
    @GetMapping("/me")
    public PublicUserResponse me(HttpSession session) { return auth.currentUser(session); }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, @RequestHeader(value = "X-CSRF-Token", required = false) String csrf) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object expected = session.getAttribute(PublicAuthService.SESSION_CSRF);
            if (!(expected instanceof String token) || csrf == null || !java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8), csrf.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }
}
