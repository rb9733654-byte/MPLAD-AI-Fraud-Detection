package com.mplad.fraud_detection.repository;

import java.util.List;

import com.mplad.fraud_detection.entity.ProjectHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectHistoryRepository extends JpaRepository<ProjectHistory, Long> {

    List<ProjectHistory> findByProjectIdOrderByUpdateDateAsc(Long projectId);

    List<ProjectHistory> findByProjectIdOrderByUpdateDateAscIdAsc(Long projectId);
}
