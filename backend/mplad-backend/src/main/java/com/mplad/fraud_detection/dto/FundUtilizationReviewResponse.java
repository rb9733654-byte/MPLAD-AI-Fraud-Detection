package com.mplad.fraud_detection.dto;

/** Non-accusatory financial monitoring signal for the authority review queue. */
public record FundUtilizationReviewResponse(
        Long projectId,
        String projectCode,
        String projectType,
        String state,
        String reviewSignal,
        String reviewReason
) {
}
