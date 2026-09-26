package com.mplad.fraud_detection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mplad.fraud_detection.dto.ProgressEvidenceResponse;
import com.mplad.fraud_detection.entity.ProgressEvidence;
import com.mplad.fraud_detection.entity.Project;
import com.mplad.fraud_detection.entity.ProjectHistory;
import com.mplad.fraud_detection.repository.ProgressEvidenceRepository;
import com.mplad.fraud_detection.repository.ProjectHistoryRepository;
import com.mplad.fraud_detection.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProgressEvidenceServiceTests {
    @Mock private ProjectRepository projects;
    @Mock private ProgressEvidenceRepository evidence;
    @Mock private ProjectHistoryRepository history;
    private Project project;
    private ProgressEvidenceService service;

    @BeforeEach void setUp() {
        project = new Project(); project.setId(2L); project.setProjectId("DEMO-MPL-1002");
        project.setContractorId("DEMO-CONTRACTOR-01"); project.setLatitude(new BigDecimal("20.2980000"));
        project.setLongitude(new BigDecimal("85.8260000")); project.setCompletionPercentage(new BigDecimal("100.00"));
        project.setActualExpenditure(new BigDecimal("40.00")); project.setReleasedAmount(new BigDecimal("80.00"));
        project.setExpenditurePerCompletionPercent(new BigDecimal("0.4000")); project.setIsCompleted(true);
        project.setLastAnalysisScore(new BigDecimal("24.00")); project.setLastAnalysisLevel("LOW");
        when(projects.findById(2L)).thenReturn(Optional.of(project));
        service = new ProgressEvidenceService(projects, evidence, history, new BigDecimal("500"), "target/test-evidence");
    }

    @Test void acceptedSeventyPercentUpdateSynchronizesPersistedProjectStateAndExpenditure() {
        returnEvidenceWhenSaved();
        ProgressEvidenceResponse response = service.submit(2L, "DEMO-CONTRACTOR-01", project.getLatitude(), project.getLongitude(),
                new BigDecimal("70.00"), new BigDecimal("65.00"), null);

        assertThat(response.acceptedForProjectState()).isTrue();
        assertThat(project.getCompletionPercentage()).isEqualByComparingTo("70.00");
        assertThat(project.getIsCompleted()).isFalse();
        assertThat(project.getActualExpenditure()).isEqualByComparingTo("65.00");
        verify(projects).save(project);
        org.mockito.ArgumentCaptor<ProjectHistory> snapshot = org.mockito.ArgumentCaptor.forClass(ProjectHistory.class);
        verify(history).save(snapshot.capture());
        assertThat(snapshot.getValue().getCompletionPercentage()).isEqualByComparingTo("100.00");
        assertThat(snapshot.getValue().getExpenditure()).isEqualByComparingTo("40.00");
        assertThat(snapshot.getValue().getStatus()).isEqualTo("Completed");
        assertThat(snapshot.getValue().getIndicatorScore()).isEqualByComparingTo("24.00");
        assertThat(snapshot.getValue().getIndicatorLevel()).isEqualTo("LOW");
        assertThat(project.getLastAnalysisScore()).isNull();
        assertThat(project.getLastAnalysisLevel()).isNull();
    }

    @Test void oneHundredPercentIsTheOnlyCompletedState() {
        returnEvidenceWhenSaved();
        service.submit(2L, "DEMO-CONTRACTOR-01", project.getLatitude(), project.getLongitude(),
                new BigDecimal("99.00"), null, null);
        assertThat(project.getIsCompleted()).isFalse();
        service.submit(2L, "DEMO-CONTRACTOR-01", project.getLatitude(), project.getLongitude(),
                new BigDecimal("100.00"), null, null);
        assertThat(project.getIsCompleted()).isTrue();
    }

    @Test void locationMismatchRemainsInHistoryButDoesNotReplaceCurrentState() {
        returnEvidenceWhenSaved();
        ProgressEvidenceResponse response = service.submit(2L, "DEMO-CONTRACTOR-01", new BigDecimal("28.0000000"),
                new BigDecimal("80.0000000"), new BigDecimal("70.00"), new BigDecimal("60.00"), null);

        assertThat(response.acceptedForProjectState()).isFalse();
        assertThat(response.verificationStatus()).startsWith("Location Mismatch");
        assertThat(project.getCompletionPercentage()).isEqualByComparingTo("100.00");
        assertThat(project.getActualExpenditure()).isEqualByComparingTo("40.00");
        assertThat(project.getLastAnalysisScore()).isEqualByComparingTo("24.00");
        assertThat(project.getLastAnalysisLevel()).isEqualTo("LOW");
        verify(projects, never()).save(any(Project.class));
        verify(history, never()).save(any(ProjectHistory.class));
        verify(evidence).save(any(ProgressEvidence.class));
    }

    @Test void missingLocationRemainsUnverifiedAndDoesNotReplaceCurrentState() {
        returnEvidenceWhenSaved();
        ProgressEvidenceResponse response = service.submit(2L, "DEMO-CONTRACTOR-01", null, null,
                new BigDecimal("70.00"), null, null);
        assertThat(response.verificationStatus()).isEqualTo("Verification Required");
        assertThat(response.acceptedForProjectState()).isFalse();
        assertThat(project.getCompletionPercentage()).isEqualByComparingTo("100.00");
        verify(projects, never()).save(any(Project.class));
        verify(history, never()).save(any(ProjectHistory.class));
        verify(evidence).save(any(ProgressEvidence.class));
    }

    @Test void rejectsExpenditureAboveReleasedFunds() {
        assertThatThrownBy(() -> service.submit(2L, "DEMO-CONTRACTOR-01", null, null, new BigDecimal("70"),
                new BigDecimal("81"), null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed funds released");
        verify(evidence, never()).save(any(ProgressEvidence.class));
    }

    @Test void contractorCannotUpdateAProjectAssignedToSomeoneElse() {
        assertThatThrownBy(() -> service.submit(2L, "DEMO-CONTRACTOR-02", null, null, new BigDecimal("70"), null, null))
                .isInstanceOf(SecurityException.class);
        verify(evidence, never()).save(any(ProgressEvidence.class));
    }

    private void returnEvidenceWhenSaved() {
        org.mockito.Mockito.doAnswer(invocation -> invocation.getArgument(0)).when(evidence).save(any(ProgressEvidence.class));
    }
}
