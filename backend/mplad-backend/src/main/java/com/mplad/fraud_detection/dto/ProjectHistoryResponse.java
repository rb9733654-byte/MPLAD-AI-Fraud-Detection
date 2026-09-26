package com.mplad.fraud_detection.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.mplad.fraud_detection.entity.ProjectHistory;

/** API representation of one chronological project progress update. */
public record ProjectHistoryResponse(
        Long id,
        LocalDate updateDate,
        BigDecimal completionPercentage,
        BigDecimal expenditure,
        Integer delayDays,
        String status,
        BigDecimal indicatorScore,
        String indicatorLevel
) {
    public static ProjectHistoryResponse from(ProjectHistory history) {
        return new ProjectHistoryResponse(
                history.getId(),
                history.getUpdateDate(),
                history.getCompletionPercentage(),
                history.getExpenditure(),
                history.getDelayDays(),
                history.getStatus(),
                history.getIndicatorScore(),
                history.getIndicatorLevel()
        );
    }
}
