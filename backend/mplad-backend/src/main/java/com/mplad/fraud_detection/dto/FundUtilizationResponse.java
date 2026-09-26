package com.mplad.fraud_detection.dto;

import java.math.BigDecimal;
import java.util.List;

/** Authoritative financial-monitoring summary for one project. */
public record FundUtilizationResponse(
        Long projectId,
        BigDecimal sanctionedAmount,
        BigDecimal releasedAmount,
        BigDecimal actualExpenditure,
        BigDecimal remainingAmount,
        BigDecimal utilizationPercentage,
        String reviewSignal,
        String reviewReason,
        boolean financialDataValid,
        List<FundUtilizationTrendPoint> expenditureTrend
) {
}
