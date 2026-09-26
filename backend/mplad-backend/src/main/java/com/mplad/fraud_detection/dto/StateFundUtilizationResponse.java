package com.mplad.fraud_detection.dto;

import java.math.BigDecimal;

/** Aggregated fictional-demo financial position for one state. */
public record StateFundUtilizationResponse(
        String state,
        BigDecimal totalReleasedAmount,
        BigDecimal totalActualExpenditure,
        BigDecimal utilizationPercentage,
        boolean financialDataValid
) {
}
