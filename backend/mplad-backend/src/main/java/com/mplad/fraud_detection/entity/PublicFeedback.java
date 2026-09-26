package com.mplad.fraud_detection.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "public_feedback")
public class PublicFeedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "project_id", nullable = false) private Project project;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "public_user_id", nullable = false) private PublicUser publicUser;
    @Column(nullable = false) private int rating;
    @Column(nullable = false, length = 1000) private String comment;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    public Long getId() { return id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public PublicUser getPublicUser() { return publicUser; }
    public void setPublicUser(PublicUser publicUser) { this.publicUser = publicUser; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
