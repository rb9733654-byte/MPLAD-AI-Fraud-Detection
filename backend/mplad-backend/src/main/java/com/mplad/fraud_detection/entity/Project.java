package com.mplad.fraud_detection.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "project_id", nullable = false, unique = true, length = 50)
    private String projectId;

    @Column(name = "constituency", nullable = false, length = 120)
    private String constituency;

    @Column(name = "district", nullable = false, length = 120)
    private String district;

    /** Fictional demo geography used only for portfolio-level aggregation. */
    @Column(name = "state", nullable = false, length = 120)
    private String state;

    @Column(name = "contractor_id", nullable = false, length = 50)
    private String contractorId;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "project_type", nullable = false, length = 80)
    private String projectType;

    @Column(name = "sanctioned_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal sanctionedAmount;

    @Column(name = "released_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal releasedAmount;

    @Column(name = "actual_expenditure", nullable = false, precision = 15, scale = 2)
    private BigDecimal actualExpenditure;

    @Column(name = "completion_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal completionPercentage;

    @Column(name = "project_duration_days", nullable = false)
    private Integer projectDurationDays;

    @Column(name = "delay_days", nullable = false)
    private Integer delayDays;

    @Column(name = "contractor_previous_projects", nullable = false)
    private Integer contractorPreviousProjects;

    @Column(name = "contractor_avg_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal contractorAvgCost;

    @Column(name = "expenditure_per_completion_percent", nullable = false, precision = 18, scale = 4)
    private BigDecimal expenditurePerCompletionPercent;

    @Column(name = "projects_by_contractor", nullable = false)
    private Integer projectsByContractor;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted;

    @Column(name = "last_analysis_score", precision = 5, scale = 2)
    private BigDecimal lastAnalysisScore;

    @Column(name = "last_analysis_level", length = 20)
    private String lastAnalysisLevel;

    // These values are assigned by MySQL defaults and the ON UPDATE rule.
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Project() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getConstituency() {
        return constituency;
    }

    public void setConstituency(String constituency) {
        this.constituency = constituency;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getContractorId() { return contractorId; }
    public void setContractorId(String contractorId) { this.contractorId = contractorId; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public BigDecimal getSanctionedAmount() {
        return sanctionedAmount;
    }

    public void setSanctionedAmount(BigDecimal sanctionedAmount) {
        this.sanctionedAmount = sanctionedAmount;
    }

    public BigDecimal getReleasedAmount() {
        return releasedAmount;
    }

    public void setReleasedAmount(BigDecimal releasedAmount) {
        this.releasedAmount = releasedAmount;
    }

    public BigDecimal getActualExpenditure() {
        return actualExpenditure;
    }

    public void setActualExpenditure(BigDecimal actualExpenditure) {
        this.actualExpenditure = actualExpenditure;
    }

    public BigDecimal getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(BigDecimal completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public Integer getProjectDurationDays() {
        return projectDurationDays;
    }

    public void setProjectDurationDays(Integer projectDurationDays) {
        this.projectDurationDays = projectDurationDays;
    }

    public Integer getDelayDays() {
        return delayDays;
    }

    public void setDelayDays(Integer delayDays) {
        this.delayDays = delayDays;
    }

    public Integer getContractorPreviousProjects() {
        return contractorPreviousProjects;
    }

    public void setContractorPreviousProjects(Integer contractorPreviousProjects) {
        this.contractorPreviousProjects = contractorPreviousProjects;
    }

    public BigDecimal getContractorAvgCost() {
        return contractorAvgCost;
    }

    public void setContractorAvgCost(BigDecimal contractorAvgCost) {
        this.contractorAvgCost = contractorAvgCost;
    }

    public BigDecimal getExpenditurePerCompletionPercent() {
        return expenditurePerCompletionPercent;
    }

    public void setExpenditurePerCompletionPercent(BigDecimal expenditurePerCompletionPercent) {
        this.expenditurePerCompletionPercent = expenditurePerCompletionPercent;
    }

    public Integer getProjectsByContractor() {
        return projectsByContractor;
    }

    public void setProjectsByContractor(Integer projectsByContractor) {
        this.projectsByContractor = projectsByContractor;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public BigDecimal getLastAnalysisScore() { return lastAnalysisScore; }
    public void setLastAnalysisScore(BigDecimal lastAnalysisScore) { this.lastAnalysisScore = lastAnalysisScore; }
    public String getLastAnalysisLevel() { return lastAnalysisLevel; }
    public void setLastAnalysisLevel(String lastAnalysisLevel) { this.lastAnalysisLevel = lastAnalysisLevel; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
