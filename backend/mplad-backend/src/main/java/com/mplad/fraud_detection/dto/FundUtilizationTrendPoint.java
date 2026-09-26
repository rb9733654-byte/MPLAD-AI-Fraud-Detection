package com.mplad.fraud_detection.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One dated expenditure reading for a project's utilization trend. */
public record FundUtilizationTrendPoint(
        LocalDate updateDate,
        BigDecimal expenditure,
        BigDecimal utilizationPercentage,
        BigDecimal completionPercentage
) {
}
