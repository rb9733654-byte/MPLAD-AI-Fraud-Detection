package com.mplad.fraud_detection.repository;
import com.mplad.fraud_detection.entity.PublicFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PublicFeedbackRepository extends JpaRepository<PublicFeedback, Long> {
    List<PublicFeedback> findByProjectIdOrderByIdDesc(Long projectId);
    boolean existsByProjectIdAndPublicUserId(Long projectId, Long userId);
}
