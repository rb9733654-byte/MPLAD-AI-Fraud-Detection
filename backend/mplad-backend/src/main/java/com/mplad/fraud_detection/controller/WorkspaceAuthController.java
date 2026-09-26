package com.mplad.fraud_detection.controller;

import com.mplad.fraud_detection.dto.WorkspaceAuthRequest;
import com.mplad.fraud_detection.dto.WorkspaceUserResponse;
import com.mplad.fraud_detection.service.WorkspaceAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/workspace-auth")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:5502", "http://127.0.0.1:5502"}, allowCredentials = "true")
public class WorkspaceAuthController {
    private final WorkspaceAuthService auth;
    public WorkspaceAuthController(WorkspaceAuthService auth) { this.auth = auth; }

    @PostMapping("/{role}/register")
    public ResponseEntity<Map<String, String>> register(@PathVariable String role, @RequestBody WorkspaceAuthRequest request) {
        String normalizedRole = normalizeRole(role);
        auth.register(normalizedRole, request.identity(), request.username(), request.password(), request.confirmPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Demo workspace account created. Sign in to continue."));
    }
    @PostMapping("/{role}/login")
    public WorkspaceUserResponse login(@PathVariable String role, @RequestBody WorkspaceAuthRequest request, HttpServletRequest httpRequest) {
        return auth.login(normalizeRole(role), request.identity(), request.password(), httpRequest);
    }
    @GetMapping("/{role}/me")
    public WorkspaceUserResponse me(@PathVariable String role, HttpSession session) { return auth.current(normalizeRole(role), session); }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, @RequestHeader(value = "X-CSRF-Token", required = false) String csrf) {
        auth.logout(request, csrf); return ResponseEntity.noContent().build();
    }
    private String normalizeRole(String role) {
        return switch (role.toLowerCase(java.util.Locale.ROOT)) {
            case "authority" -> "AUTHORITY";
            case "contractor" -> "CONTRACTOR";
            default -> throw new IllegalArgumentException("Unsupported workspace role.");
        };
    }
}
