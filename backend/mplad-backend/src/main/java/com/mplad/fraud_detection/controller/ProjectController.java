package com.mplad.fraud_detection.controller;

import java.util.List;

import com.mplad.fraud_detection.dto.AiPredictionResponse;
import com.mplad.fraud_detection.dto.FundUtilizationResponse;
import com.mplad.fraud_detection.dto.ProjectHistoryResponse;
import com.mplad.fraud_detection.entity.Project;
import com.mplad.fraud_detection.service.ProjectAnalysisService;
import com.mplad.fraud_detection.service.FundUtilizationService;
import com.mplad.fraud_detection.service.ProjectHistoryService;
import com.mplad.fraud_detection.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = {
        "http://localhost:3000",
        "http://localhost:5173",
        "http://localhost:5500",
        "http://localhost:5502",
        "http://localhost:8000",
        "http://127.0.0.1:5500",
        "http://127.0.0.1:5502",
        "http://127.0.0.1:8000"
}, allowCredentials = "true")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectAnalysisService projectAnalysisService;
    private final ProjectHistoryService projectHistoryService;
    private final FundUtilizationService fundUtilizationService;

    public ProjectController(
            ProjectService projectService,
            ProjectAnalysisService projectAnalysisService,
            ProjectHistoryService projectHistoryService,
            FundUtilizationService fundUtilizationService
    ) {
        this.projectService = projectService;
        this.projectAnalysisService = projectAnalysisService;
        this.projectHistoryService = projectHistoryService;
        this.fundUtilizationService = fundUtilizationService;
    }

    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/analyze")
    public AiPredictionResponse analyzeProject(@PathVariable Long id) {
        return projectAnalysisService.analyzeProject(id);
    }

    @GetMapping("/{id}/history")
    public List<ProjectHistoryResponse> getProjectHistory(@PathVariable Long id) {
        return projectHistoryService.getHistory(id);
    }

    @GetMapping("/{id}/fund-utilization")
    public FundUtilizationResponse getFundUtilization(@PathVariable Long id) {
        return fundUtilizationService.getProjectUtilization(id);
    }

    @GetMapping("/district/{district}")
    public List<Project> getProjectsByDistrict(@PathVariable String district) {
        return projectService.getProjectsByDistrict(district);
    }

    @GetMapping("/type/{projectType}")
    public List<Project> getProjectsByProjectType(@PathVariable String projectType) {
        return projectService.getProjectsByProjectType(projectType);
    }

    @GetMapping("/completed")
    public List<Project> getCompletedProjects() {
        return projectService.getCompletedProjects();
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        Project savedProject = projectService.saveProject(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProject);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        if (projectService.getProjectById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
