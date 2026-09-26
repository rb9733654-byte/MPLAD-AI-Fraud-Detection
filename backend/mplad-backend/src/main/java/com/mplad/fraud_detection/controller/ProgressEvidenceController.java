package com.mplad.fraud_detection.controller;

import com.mplad.fraud_detection.dto.ProgressEvidenceResponse;
import com.mplad.fraud_detection.entity.Project;
import com.mplad.fraud_detection.service.ProgressEvidenceService;
import com.mplad.fraud_detection.service.WorkspaceAuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins={"http://localhost:5500","http://127.0.0.1:5500","http://localhost:5501","http://127.0.0.1:5501","http://localhost:5502","http://127.0.0.1:5502"}, allowCredentials="true")
public class ProgressEvidenceController {
    private final ProgressEvidenceService service;
    public ProgressEvidenceController(ProgressEvidenceService service) { this.service = service; }
    @GetMapping("/contractors/{contractorId}/projects")
    public List<Project> assigned(@PathVariable String contractorId, HttpSession session) {
        if (!contractorId.equals(session.getAttribute(WorkspaceAuthService.IDENTITY))) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        return service.assigned(contractorId);
    }
    @GetMapping("/projects/{id}/progress-evidence")
    public List<ProgressEvidenceResponse> evidence(@PathVariable Long id) { return service.forProject(id); }
    @PostMapping(value="/projects/{id}/progress-evidence", consumes="multipart/form-data")
    public ProgressEvidenceResponse submit(@PathVariable Long id, @RequestParam String contractorId,
            @RequestParam(required=false) BigDecimal latitude, @RequestParam(required=false) BigDecimal longitude,
            @RequestParam(required=false) BigDecimal completionPercentage, @RequestParam(required=false) BigDecimal expenditure,
            @RequestPart(required=false) MultipartFile photo, HttpSession session) {
        if (!contractorId.equals(session.getAttribute(WorkspaceAuthService.IDENTITY))) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        return service.submit(id, contractorId, latitude, longitude, completionPercentage, expenditure, photo);
    }
}
