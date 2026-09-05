package com.mplad.fraud_detection.service;

import java.util.List;
import java.util.Optional;

import com.mplad.fraud_detection.entity.Project;
import com.mplad.fraud_detection.repository.ProjectRepository;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    public List<Project> getProjectsByDistrict(String district) {
        return projectRepository.findByDistrict(district);
    }

    public List<Project> getProjectsByProjectType(String projectType) {
        return projectRepository.findByProjectType(projectType);
    }

    public List<Project> getCompletedProjects() {
        return projectRepository.findByIsCompleted(true);
    }

    public Project saveProject(Project project) {
        return projectRepository.save(project);
    }

    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }
}
