package com.mplad.fraud_detection.repository;

import java.util.List;

import com.mplad.fraud_detection.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByDistrict(String district);

    List<Project> findByProjectType(String projectType);

    List<Project> findByIsCompleted(Boolean isCompleted);
}
