package com.mplad.fraud_detection.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Dynamic anomaly-review result returned by the local Python service. */
public record AiPredictionResponse(
        @JsonProperty("anomaly_status") String anomalyStatus,
        @JsonProperty("anomaly_score") BigDecimal anomalyScore,
        @JsonProperty("risk_level") String riskLevel,
        @JsonProperty("model_output") String modelOutput
) {
}
