package com.mplad.fraud_detection.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;

/** Supporting progress evidence; geo-location is a review signal, not photo-authenticity proof. */
@Entity
@Table(name = "progress_evidence")
public class ProgressEvidence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id") private Project project;
    @Column(name = "contractor_id", nullable = false) private String contractorId;
    @Column(name = "photo_reference") private String photoReference;
    @Column(name = "submitted_latitude", precision = 10, scale = 7) private BigDecimal submittedLatitude;
    @Column(name = "submitted_longitude", precision = 10, scale = 7) private BigDecimal submittedLongitude;
    @Column(name = "distance_metres", precision = 12, scale = 2) private BigDecimal distanceMetres;
    @Column(name = "verification_status", nullable = false) private String verificationStatus;
    @Column(name = "submitted_at", nullable = false) private LocalDateTime submittedAt;
    @Column(name = "completion_percentage", precision = 5, scale = 2) private BigDecimal completionPercentage;
    @Column(name = "expenditure", precision = 15, scale = 2) private BigDecimal expenditure;
    @Column(name = "accepted_for_project_state", nullable = false) private boolean acceptedForProjectState;
    public Long getId(){return id;} public Project getProject(){return project;} public String getContractorId(){return contractorId;} public String getPhotoReference(){return photoReference;} public BigDecimal getSubmittedLatitude(){return submittedLatitude;} public BigDecimal getSubmittedLongitude(){return submittedLongitude;} public BigDecimal getDistanceMetres(){return distanceMetres;} public String getVerificationStatus(){return verificationStatus;} public LocalDateTime getSubmittedAt(){return submittedAt;} public BigDecimal getCompletionPercentage(){return completionPercentage;} public BigDecimal getExpenditure(){return expenditure;} public boolean isAcceptedForProjectState(){return acceptedForProjectState;}
    public void setProject(Project v){project=v;} public void setContractorId(String v){contractorId=v;} public void setPhotoReference(String v){photoReference=v;} public void setSubmittedLatitude(BigDecimal v){submittedLatitude=v;} public void setSubmittedLongitude(BigDecimal v){submittedLongitude=v;} public void setDistanceMetres(BigDecimal v){distanceMetres=v;} public void setVerificationStatus(String v){verificationStatus=v;} public void setSubmittedAt(LocalDateTime v){submittedAt=v;} public void setCompletionPercentage(BigDecimal v){completionPercentage=v;} public void setExpenditure(BigDecimal v){expenditure=v;} public void setAcceptedForProjectState(boolean v){acceptedForProjectState=v;}
}
