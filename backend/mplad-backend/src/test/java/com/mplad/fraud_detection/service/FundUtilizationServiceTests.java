package com.mplad.fraud_detection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.mplad.fraud_detection.dto.FundUtilizationResponse;
import com.mplad.fraud_detection.entity.Project;
import com.mplad.fraud_detection.entity.ProjectHistory;
import com.mplad.fraud_detection.repository.ProjectHistoryRepository;
import com.mplad.fraud_detection.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FundUtilizationServiceTests {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectHistoryRepository historyRepository;

    @Test
    void calculatesRemainingAndUtilizationFromReleasedAmount() {
        Project project = project("DEMO-1", "100.00", "80.00", "60.00");
        when(projectRepository.findById(1L)).thenReturn(java.util.Optional.of(project));
        when(historyRepository.findByProjectIdOrderByUpdateDateAsc(1L)).thenReturn(List.of());

        FundUtilizationResponse result = new FundUtilizationService(projectRepository, historyRepository)
                .getProjectUtilization(1L);

        assertThat(result.remainingAmount()).isEqualByComparingTo("20.00");
        assertThat(result.utilizationPercentage()).isEqualByComparingTo("75.00");
        assertThat(result.reviewSignal()).isEqualTo("Normal Utilization Pattern");
    }

    @Test
    void suggestsReviewForSuddenSpendWithoutProportionalCompletion() {
        Project project = project("DEMO-2", "100.00", "100.00", "70.00");
        when(projectRepository.findById(1L)).thenReturn(java.util.Optional.of(project));
        when(historyRepository.findByProjectIdOrderByUpdateDateAsc(1L)).thenReturn(List.of(
                history("2026-01-01", "10.00", "10.00"),
                history("2026-04-01", "20.00", "20.00"),
                history("2026-08-01", "30.00", "70.00")
        ));

        FundUtilizationResponse result = new FundUtilizationService(projectRepository, historyRepository)
                .getProjectUtilization(1L);

        assertThat(result.reviewSignal()).isEqualTo("Unusual Utilization Pattern — Review Suggested");
        assertThat(result.expenditureTrend()).hasSize(3);
    }

    @Test
    void marksExpenditureAboveReleasedAmountAsInvalidData() {
        Project project = project("DEMO-3", "100.00", "50.00", "60.00");
        when(projectRepository.findById(1L)).thenReturn(java.util.Optional.of(project));
        when(historyRepository.findByProjectIdOrderByUpdateDateAsc(1L)).thenReturn(List.of());

        FundUtilizationResponse result = new FundUtilizationService(projectRepository, historyRepository)
                .getProjectUtilization(1L);

        assertThat(result.financialDataValid()).isFalse();
        assertThat(result.reviewSignal()).isEqualTo("Financial Data Needs Review");
    }

    @Test
    void marksMissingFinancialValuesAsInvalidData() {
        Project project = project("DEMO-4", "100.00", "50.00", "20.00");
        project.setSanctionedAmount(null);
        when(projectRepository.findById(1L)).thenReturn(java.util.Optional.of(project));
        when(historyRepository.findByProjectIdOrderByUpdateDateAsc(1L)).thenReturn(List.of());

        FundUtilizationResponse result = new FundUtilizationService(projectRepository, historyRepository)
                .getProjectUtilization(1L);

        assertThat(result.financialDataValid()).isFalse();
        assertThat(result.reviewSignal()).isEqualTo("Financial Data Needs Review");
    }

    private Project project(String code, String sanctioned, String released, String expenditure) {
        Project project = new Project();
        project.setId(1L);
        project.setProjectId(code);
        project.setState("Demo State");
        project.setSanctionedAmount(new BigDecimal(sanctioned));
        project.setReleasedAmount(new BigDecimal(released));
        project.setActualExpenditure(new BigDecimal(expenditure));
        project.setIsCompleted(false);
        return project;
    }

    private ProjectHistory history(String date, String completion, String expenditure) {
        ProjectHistory history = new ProjectHistory();
        history.setUpdateDate(LocalDate.parse(date));
        history.setCompletionPercentage(new BigDecimal(completion));
        history.setExpenditure(new BigDecimal(expenditure));
        return history;
    }
}
