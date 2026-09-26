package com.mplad.fraud_detection.service;

import com.mplad.fraud_detection.dto.AiPredictionRequest;
import com.mplad.fraud_detection.dto.AiPredictionResponse;
import com.mplad.fraud_detection.entity.Project;
import com.mplad.fraud_detection.repository.ProjectRepository;
import org.springframework.stereotype.Service;

/** Coordinates project lookup and an on-demand anomaly-review prediction. */
@Service
public class ProjectAnalysisService {

    private final ProjectRepository projectRepository;
    private final PythonAnomalyClient pythonAnomalyClient;

    public ProjectAnalysisService(ProjectRepository projectRepository, PythonAnomalyClient pythonAnomalyClient) {
        this.projectRepository = projectRepository;
        this.pythonAnomalyClient = pythonAnomalyClient;
    }

    public AiPredictionResponse analyzeProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        AiPredictionResponse result = pythonAnomalyClient.predict(AiPredictionRequest.from(project));
        project.setLastAnalysisScore(result.anomalyScore());
        project.setLastAnalysisLevel(result.riskLevel());
        projectRepository.save(project);
        return result;
    }

    public static class ProjectNotFoundException extends RuntimeException {
        public ProjectNotFoundException(Long projectId) {
            super("Project " + projectId + " was not found.");
        }
    }
}
