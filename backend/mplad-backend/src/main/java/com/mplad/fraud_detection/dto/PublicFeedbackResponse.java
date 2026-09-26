package com.mplad.fraud_detection.dto;
import com.mplad.fraud_detection.entity.PublicFeedback;
import java.time.LocalDateTime;
public record PublicFeedbackResponse(Integer rating, String comment, LocalDateTime createdAt) {
    public static PublicFeedbackResponse from(PublicFeedback f) { return new PublicFeedbackResponse(f.getRating(), f.getComment(), f.getCreatedAt()); }
}
