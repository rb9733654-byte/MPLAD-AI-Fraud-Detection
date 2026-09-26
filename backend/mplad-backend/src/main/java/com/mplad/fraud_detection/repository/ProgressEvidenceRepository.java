package com.mplad.fraud_detection.repository;
import java.util.List;
import com.mplad.fraud_detection.entity.ProgressEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProgressEvidenceRepository extends JpaRepository<ProgressEvidence, Long> { List<ProgressEvidence> findByProjectIdOrderBySubmittedAtDescIdDesc(Long projectId); }
