package com.mplad.fraud_detection.service;

import com.mplad.fraud_detection.dto.NotificationResponse;
import com.mplad.fraud_detection.dto.ReviewRequest;
import com.mplad.fraud_detection.dto.ReviewResponse;
import com.mplad.fraud_detection.entity.AuthorityReview;
import com.mplad.fraud_detection.entity.ContractorNotification;
import com.mplad.fraud_detection.entity.Project;
import com.mplad.fraud_detection.repository.AuthorityReviewRepository;
import com.mplad.fraud_detection.repository.ContractorNotificationRepository;
import com.mplad.fraud_detection.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ReviewService {
    private static final Set<String> STATES = Set.of("Reviewed", "Monitoring Required", "Progress Update Required", "Further Verification Required", "Closed / No Further Action");
    private final ProjectRepository projects;
    private final AuthorityReviewRepository reviews;
    private final ContractorNotificationRepository notifications;
    public ReviewService(ProjectRepository projects, AuthorityReviewRepository reviews, ContractorNotificationRepository notifications) {
        this.projects = projects; this.reviews = reviews; this.notifications = notifications;
    }

    @Transactional
    public ReviewResponse create(Long id, ReviewRequest request) {
        Project project = projects.findById(id).orElseThrow(() -> new IllegalArgumentException("Project not found."));
        if (request == null || !STATES.contains(request.reviewStatus()) || request.remark() == null || request.remark().isBlank()
                || request.remark().length() > 1000 || (request.requiredAction() != null && request.requiredAction().length() > 300))
            throw new IllegalArgumentException("Review status or remark is invalid.");
        AuthorityReview review = new AuthorityReview(); review.setProject(project); review.setReviewStatus(request.reviewStatus());
        review.setRemark(request.remark().trim()); review.setRequiredAction(request.requiredAction() == null ? null : request.requiredAction().trim());
        review.setReviewDate(LocalDateTime.now()); review = reviews.save(review);
        boolean notified = project.getContractorId() != null && !project.getContractorId().isBlank();
        if (notified) {
            ContractorNotification notification = new ContractorNotification();
            notification.setContractorId(project.getContractorId()); notification.setProject(project); notification.setReview(review);
            notification.setTitle("New Project Review");
            notification.setMessage("Project " + project.getProjectId() + ": " + review.getReviewStatus() + ". " + review.getRemark()
                    + (review.getRequiredAction() == null || review.getRequiredAction().isBlank() ? "" : " Required action: " + review.getRequiredAction()));
            notification.setNotificationType("AUTHORITY_REVIEW"); notification.setRead(false); notification.setCreatedAt(review.getReviewDate());
            notifications.save(notification);
        }
        return ReviewResponse.from(review, notified);
    }

    public List<ReviewResponse> list(Long id) {
        if (!projects.existsById(id)) throw new IllegalArgumentException("Project not found.");
        return reviews.findByProjectIdOrderByReviewDateDesc(id).stream().map(review -> ReviewResponse.from(review, false)).toList();
    }
    public List<NotificationResponse> notifications(String contractor) {
        return notifications.findByContractorIdOrderByCreatedAtDesc(contractor).stream().map(NotificationResponse::from).toList();
    }
    @Transactional
    public void read(Long id, String contractor) {
        ContractorNotification notification = notifications.findByIdAndContractorId(id, contractor)
                .orElseThrow(() -> new SecurityException("Notification not found."));
        notification.setRead(true); notifications.save(notification);
    }
}
