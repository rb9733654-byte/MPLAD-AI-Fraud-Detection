package com.mplad.fraud_detection.controller;

import com.mplad.fraud_detection.dto.NotificationResponse;
import com.mplad.fraud_detection.dto.ReviewRequest;
import com.mplad.fraud_detection.dto.ReviewResponse;
import com.mplad.fraud_detection.service.ReviewService;
import com.mplad.fraud_detection.service.WorkspaceAuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins={"http://localhost:5500","http://127.0.0.1:5500","http://localhost:5501","http://127.0.0.1:5501","http://localhost:5502","http://127.0.0.1:5502"}, allowCredentials="true")
public class ReviewController {
    private final ReviewService service;
    public ReviewController(ReviewService service) { this.service = service; }

    @PostMapping("/projects/{id}/reviews")
    public ReviewResponse create(@PathVariable Long id, @RequestBody ReviewRequest request, HttpSession session) {
        WorkspaceAuthService.requireRole(session, "AUTHORITY"); return service.create(id, request);
    }
    @GetMapping("/projects/{id}/reviews")
    public List<ReviewResponse> list(@PathVariable Long id, HttpSession session) {
        WorkspaceAuthService.requireRole(session, "AUTHORITY"); return service.list(id);
    }
    @GetMapping("/contractors/{id}/notifications")
    public List<NotificationResponse> notifications(@PathVariable String id, HttpSession session) {
        WorkspaceAuthService.requireRole(session, "CONTRACTOR");
        if (!id.equals(session.getAttribute(WorkspaceAuthService.IDENTITY))) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        return service.notifications(id);
    }
    @PatchMapping("/contractors/{contractorId}/notifications/{id}/read")
    public void read(@PathVariable String contractorId, @PathVariable Long id, HttpSession session) {
        WorkspaceAuthService.requireRole(session, "CONTRACTOR");
        if (!contractorId.equals(session.getAttribute(WorkspaceAuthService.IDENTITY))) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        service.read(id, contractorId);
    }
}
