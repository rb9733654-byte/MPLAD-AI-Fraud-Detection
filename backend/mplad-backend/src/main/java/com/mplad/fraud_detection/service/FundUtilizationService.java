package com.mplad.fraud_detection.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.mplad.fraud_detection.dto.FundUtilizationOverviewResponse;
import com.mplad.fraud_detection.dto.FundUtilizationResponse;
import com.mplad.fraud_detection.dto.FundUtilizationReviewResponse;
import com.mplad.fraud_detection.dto.FundUtilizationTrendPoint;
import com.mplad.fraud_detection.dto.StateFundUtilizationResponse;
import com.mplad.fraud_detection.entity.Project;
import com.mplad.fraud_detection.entity.ProjectHistory;
import com.mplad.fraud_detection.repository.ProjectHistoryRepository;
import com.mplad.fraud_detection.repository.ProjectRepository;
import org.springframework.stereotype.Service;

/** Calculates financial monitoring values separately from the AI anomaly model. */
@Service
public class FundUtilizationService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal LOW_UTILIZATION_THRESHOLD = BigDecimal.valueOf(25);
    private static final BigDecimal SUDDEN_INCREASE_THRESHOLD = BigDecimal.valueOf(30);
    private static final BigDecimal LIMITED_PROGRESS_THRESHOLD = BigDecimal.valueOf(15);

    private final ProjectRepository projectRepository;
    private final ProjectHistoryRepository historyRepository;

    public FundUtilizationService(ProjectRepository projectRepository, ProjectHistoryRepository historyRepository) {
        this.projectRepository = projectRepository;
        this.historyRepository = historyRepository;
    }

    public FundUtilizationResponse getProjectUtilization(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new FundUtilizationProjectNotFoundException(projectId));
        List<ProjectHistory> history = historyRepository.findByProjectIdOrderByUpdateDateAsc(projectId);
        FinancialPosition position = calculatePosition(project.getReleasedAmount(), project.getActualExpenditure());
        List<FundUtilizationTrendPoint> trend = history.stream()
                .map(item -> new FundUtilizationTrendPoint(
                        item.getUpdateDate(),
                        item.getExpenditure(),
                        percentage(item.getExpenditure(), project.getReleasedAmount()),
                        item.getCompletionPercentage()))
                .toList();
        ReviewSignal review = determineReviewSignal(project, history, position);
        return new FundUtilizationResponse(project.getId(), project.getSanctionedAmount(), project.getReleasedAmount(),
                project.getActualExpenditure(), position.remaining(), position.utilization(), review.label(),
                review.reason(), position.valid() && isNonNegative(project.getSanctionedAmount()), trend);
    }

    public FundUtilizationOverviewResponse getOverview() {
        List<Project> projects = projectRepository.findAll();
        BigDecimal released = sum(projects, Project::getReleasedAmount);
        BigDecimal expenditure = sum(projects, Project::getActualExpenditure);
        List<StateFundUtilizationResponse> states = projects.stream()
                .collect(Collectors.groupingBy(project -> blankAsUnknown(project.getState())))
                .entrySet().stream()
                .map(entry -> stateSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(StateFundUtilizationResponse::state))
                .toList();
        List<FundUtilizationReviewResponse> reviews = projects.stream()
                .map(project -> {
                    ReviewSignal signal = determineReviewSignal(project,
                        historyRepository.findByProjectIdOrderByUpdateDateAsc(project.getId()),
                        calculatePosition(project.getReleasedAmount(), project.getActualExpenditure()));
                    return new FundUtilizationReviewResponse(project.getId(), project.getProjectId(), project.getProjectType(),
                            blankAsUnknown(project.getState()), signal.label(), signal.reason());
                })
                .filter(review -> !review.reviewSignal().equals("Normal Utilization Pattern"))
                .sorted(Comparator.comparing(FundUtilizationReviewResponse::projectCode))
                .toList();
        boolean financialDataValid = projects.stream().allMatch(project ->
                calculatePosition(project.getReleasedAmount(), project.getActualExpenditure()).valid()
                        && isNonNegative(project.getSanctionedAmount()));
        return new FundUtilizationOverviewResponse(released, expenditure, released.subtract(expenditure),
                percentage(expenditure, released), financialDataValid, reviews.size(), states, reviews);
    }

    private StateFundUtilizationResponse stateSummary(String state, List<Project> projects) {
        BigDecimal released = sum(projects, Project::getReleasedAmount);
        BigDecimal expenditure = sum(projects, Project::getActualExpenditure);
        return new StateFundUtilizationResponse(state, released, expenditure, percentage(expenditure, released),
                projects.stream().allMatch(project -> calculatePosition(project.getReleasedAmount(), project.getActualExpenditure()).valid()));
    }

    private ReviewSignal determineReviewSignal(Project project, List<ProjectHistory> history, FinancialPosition position) {
        if (!position.valid() || !isNonNegative(project.getSanctionedAmount())) {
            return new ReviewSignal("Financial Data Needs Review", "Released amount and expenditure need reconciliation.", true);
        }
        if (hasUnusualHistoricalPattern(history, project.getReleasedAmount())) {
            return new ReviewSignal("Unusual Utilization Pattern — Review Suggested",
                    "Expenditure increased sharply while completion changed only slightly.", true);
        }
        if (position.utilization() != null && position.utilization().compareTo(LOW_UTILIZATION_THRESHOLD) < 0
                && !Boolean.TRUE.equals(project.getIsCompleted())) {
            return new ReviewSignal("Low Utilization — Monitor",
                    "A limited share of released funds has been used so far.", true);
        }
        return new ReviewSignal("Normal Utilization Pattern", "No financial monitoring signal is currently indicated.", false);
    }

    private boolean hasUnusualHistoricalPattern(List<ProjectHistory> history, BigDecimal releasedAmount) {
        if (history.size() < 3 || !isPositive(releasedAmount)) return false;
        ProjectHistory previous = history.get(history.size() - 2);
        ProjectHistory current = history.get(history.size() - 1);
        BigDecimal expenditureChange = percentage(current.getExpenditure().subtract(previous.getExpenditure()), releasedAmount);
        BigDecimal completionChange = current.getCompletionPercentage().subtract(previous.getCompletionPercentage());
        return expenditureChange != null && expenditureChange.compareTo(SUDDEN_INCREASE_THRESHOLD) >= 0
                && completionChange.compareTo(LIMITED_PROGRESS_THRESHOLD) <= 0;
    }

    private FinancialPosition calculatePosition(BigDecimal released, BigDecimal expenditure) {
        boolean valid = isNonNegative(released) && isNonNegative(expenditure) && released.compareTo(expenditure) >= 0;
        return new FinancialPosition(released == null || expenditure == null ? null : released.subtract(expenditure),
                percentage(expenditure, released), valid);
    }

    private BigDecimal sum(List<Project> projects, java.util.function.Function<Project, BigDecimal> extractor) {
        return projects.stream().map(extractor).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal base) {
        if (amount == null || !isPositive(base)) return null;
        return amount.multiply(HUNDRED).divide(base, 2, RoundingMode.HALF_UP);
    }

    private boolean isPositive(BigDecimal value) { return value != null && value.compareTo(BigDecimal.ZERO) > 0; }
    private boolean isNonNegative(BigDecimal value) { return value != null && value.compareTo(BigDecimal.ZERO) >= 0; }
    private String blankAsUnknown(String state) { return state == null || state.isBlank() ? "Unspecified demo state" : state; }

    private record FinancialPosition(BigDecimal remaining, BigDecimal utilization, boolean valid) { }
    private record ReviewSignal(String label, String reason, boolean requiresReview) { }

    public static class FundUtilizationProjectNotFoundException extends RuntimeException {
        public FundUtilizationProjectNotFoundException(Long projectId) { super("Project " + projectId + " was not found."); }
    }
}
