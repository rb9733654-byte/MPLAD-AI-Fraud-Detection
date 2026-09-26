package com.mplad.fraud_detection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.mplad.fraud_detection.dto.AiPredictionRequest;
import com.mplad.fraud_detection.dto.AiPredictionResponse;
import com.mplad.fraud_detection.entity.Project;
import com.mplad.fraud_detection.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectAnalysisServiceTests {
    @Mock private ProjectRepository projects;
    @Mock private PythonAnomalyClient python;
    @InjectMocks private ProjectAnalysisService service;

    @Test
    void analyzesAndPersistsTheLatestProjectValues() {
        Project project = new Project();
        project.setId(2L);
        project.setSanctionedAmount(new BigDecimal("82.00"));
        project.setReleasedAmount(new BigDecimal("75.00"));
        project.setActualExpenditure(new BigDecimal("60.00"));
        project.setCompletionPercentage(new BigDecimal("70.00"));
        project.setProjectDurationDays(420);
        project.setDelayDays(12);
        project.setContractorPreviousProjects(9);
        project.setContractorAvgCost(new BigDecimal("79.20"));
        project.setExpenditurePerCompletionPercent(new BigDecimal("0.8571"));
        project.setProjectsByContractor(4);
        project.setIsCompleted(false);
        when(projects.findById(2L)).thenReturn(Optional.of(project));
        AiPredictionResponse prediction = new AiPredictionResponse("Potential Anomaly", BigDecimal.ZERO, "LOW", "Existing model result");
        when(python.predict(any(AiPredictionRequest.class))).thenReturn(prediction);

        AiPredictionResponse result = service.analyzeProject(2L);

        ArgumentCaptor<AiPredictionRequest> request = ArgumentCaptor.forClass(AiPredictionRequest.class);
        verify(python).predict(request.capture());
        assertThat(request.getValue().actualExpenditure()).isEqualByComparingTo("60.00");
        assertThat(request.getValue().completionPercentage()).isEqualByComparingTo("70.00");
        assertThat(result).isSameAs(prediction);
        assertThat(project.getLastAnalysisScore()).isEqualByComparingTo("0");
        assertThat(project.getLastAnalysisLevel()).isEqualTo("LOW");
        verify(projects).save(project);
    }
}
