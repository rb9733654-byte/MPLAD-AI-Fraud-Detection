package com.mplad.fraud_detection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mplad.fraud_detection.dto.ReviewRequest;
import com.mplad.fraud_detection.entity.AuthorityReview;
import com.mplad.fraud_detection.entity.ContractorNotification;
import com.mplad.fraud_detection.entity.Project;
import com.mplad.fraud_detection.repository.AuthorityReviewRepository;
import com.mplad.fraud_detection.repository.ContractorNotificationRepository;
import com.mplad.fraud_detection.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTests {
    @Mock private ProjectRepository projects;
    @Mock private AuthorityReviewRepository reviews;
    @Mock private ContractorNotificationRepository notifications;

    @Test void authorityReviewCreatesNotificationForAssignedProjectContractor() {
        Project project = new Project(); project.setProjectId("DEMO-MPL-1004"); project.setContractorId("DEMO-CONTRACTOR-01");
        when(projects.findById(4L)).thenReturn(java.util.Optional.of(project));
        when(reviews.save(any(AuthorityReview.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notifications.save(any(ContractorNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewService service = new ReviewService(projects, reviews, notifications);
        var result = service.create(4L, new ReviewRequest("Progress Update Required", "Please provide updated evidence.", "Submit a field update."));

        assertThat(result.notificationCreated()).isTrue();
        ArgumentCaptor<ContractorNotification> notification = ArgumentCaptor.forClass(ContractorNotification.class);
        verify(notifications).save(notification.capture());
        assertThat(notification.getValue().getContractorId()).isEqualTo("DEMO-CONTRACTOR-01");
        assertThat(notification.getValue().getProject().getProjectId()).isEqualTo("DEMO-MPL-1004");
        assertThat(notification.getValue().getReview().getReviewStatus()).isEqualTo("Progress Update Required");
    }
}
