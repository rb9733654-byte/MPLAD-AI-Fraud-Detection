package com.mplad.fraud_detection.dto;

import java.math.BigDecimal;
import java.util.List;

/** Portfolio fund-utilization totals and state-level comparison data. */
public record FundUtilizationOverviewResponse(
        BigDecimal totalReleasedAmount,
        BigDecimal totalActualExpenditure,
        BigDecimal totalRemainingAmount,
        BigDecimal overallUtilizationPercentage,
        boolean financialDataValid,
        int projectsRequiringReview,
        List<StateFundUtilizationResponse> stateUtilization,
        List<FundUtilizationReviewResponse> projectsRequiringReviewDetails
) {
}
