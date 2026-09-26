package com.mplad.fraud_detection.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A dated progress update used to provide monitoring context for one project. */
@Entity
@Table(name = "project_history")
public class ProjectHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "update_date", nullable = false)
    private LocalDate updateDate;

    @Column(name = "completion_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal completionPercentage;

    @Column(name = "expenditure", nullable = false, precision = 15, scale = 2)
    private BigDecimal expenditure;

    @Column(name = "delay_days", nullable = false)
    private Integer delayDays;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "indicator_score", precision = 5, scale = 2)
    private BigDecimal indicatorScore;

    @Column(name = "indicator_level", length = 20)
    private String indicatorLevel;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public LocalDate getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDate updateDate) {
        this.updateDate = updateDate;
    }

    public BigDecimal getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(BigDecimal completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public BigDecimal getExpenditure() {
        return expenditure;
    }

    public void setExpenditure(BigDecimal expenditure) {
        this.expenditure = expenditure;
    }

    public Integer getDelayDays() {
        return delayDays;
    }

    public void setDelayDays(Integer delayDays) {
        this.delayDays = delayDays;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getIndicatorScore() { return indicatorScore; }
    public void setIndicatorScore(BigDecimal indicatorScore) { this.indicatorScore = indicatorScore; }
    public String getIndicatorLevel() { return indicatorLevel; }
    public void setIndicatorLevel(String indicatorLevel) { this.indicatorLevel = indicatorLevel; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
