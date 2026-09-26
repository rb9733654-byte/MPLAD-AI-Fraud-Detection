package com.mplad.fraud_detection.service;

import java.util.List;

import com.mplad.fraud_detection.dto.ProjectHistoryResponse;
import com.mplad.fraud_detection.repository.ProjectHistoryRepository;
import com.mplad.fraud_detection.repository.ProjectRepository;
import org.springframework.stereotype.Service;

/** Reads chronological progress history without changing the current project record. */
@Service
public class ProjectHistoryService {

    private final ProjectRepository projectRepository;
    private final ProjectHistoryRepository projectHistoryRepository;

    public ProjectHistoryService(
            ProjectRepository projectRepository,
            ProjectHistoryRepository projectHistoryRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectHistoryRepository = projectHistoryRepository;
    }

    public List<ProjectHistoryResponse> getHistory(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectHistoryNotFoundException(projectId);
        }

        return projectHistoryRepository.findByProjectIdOrderByUpdateDateAscIdAsc(projectId).stream()
                .map(ProjectHistoryResponse::from)
                .toList();
    }

    public static class ProjectHistoryNotFoundException extends RuntimeException {
        public ProjectHistoryNotFoundException(Long projectId) {
            super("Project " + projectId + " was not found.");
        }
    }
}
