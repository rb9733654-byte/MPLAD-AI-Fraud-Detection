package com.mplad.fraud_detection.dto;
import java.math.BigDecimal; import java.time.LocalDateTime;
import com.mplad.fraud_detection.entity.ProgressEvidence;
public record ProgressEvidenceResponse(Long id, Long projectId, String contractorId, BigDecimal distanceMetres, String verificationStatus, LocalDateTime submittedAt, BigDecimal completionPercentage, BigDecimal expenditure, boolean photoAvailable, boolean acceptedForProjectState) {
 public static ProgressEvidenceResponse from(ProgressEvidence e, boolean accepted){return new ProgressEvidenceResponse(e.getId(),e.getProject().getId(),e.getContractorId(),e.getDistanceMetres(),e.getVerificationStatus(),e.getSubmittedAt(),e.getCompletionPercentage(),e.getExpenditure(),e.getPhotoReference()!=null,accepted);}
}
