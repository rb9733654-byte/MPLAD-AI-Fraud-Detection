package com.mplad.fraud_detection.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mplad.fraud_detection.entity.Project;

/** Request contract for the synthetic Python anomaly model. */
public record AiPredictionRequest(
        @JsonProperty("sanctioned_amount") BigDecimal sanctionedAmount,
        @JsonProperty("released_amount") BigDecimal releasedAmount,
        @JsonProperty("actual_expenditure") BigDecimal actualExpenditure,
        @JsonProperty("completion_percentage") BigDecimal completionPercentage,
        @JsonProperty("project_duration_days") Integer projectDurationDays,
        @JsonProperty("delay_days") Integer delayDays,
        @JsonProperty("contractor_previous_projects") Integer contractorPreviousProjects,
        @JsonProperty("contractor_avg_cost") BigDecimal contractorAvgCost,
        @JsonProperty("expenditure_per_completion_percent") BigDecimal expenditurePerCompletionPercent,
        @JsonProperty("projects_by_contractor") Integer projectsByContractor,
        @JsonProperty("is_completed") Integer isCompleted
) {
    public static AiPredictionRequest from(Project project) {
        return new AiPredictionRequest(
                project.getSanctionedAmount(),
                project.getReleasedAmount(),
                project.getActualExpenditure(),
                project.getCompletionPercentage(),
                project.getProjectDurationDays(),
                project.getDelayDays(),
                project.getContractorPreviousProjects(),
                project.getContractorAvgCost(),
                project.getExpenditurePerCompletionPercent(),
                project.getProjectsByContractor(),
                Boolean.TRUE.equals(project.getIsCompleted()) ? 1 : 0
        );
    }
}
