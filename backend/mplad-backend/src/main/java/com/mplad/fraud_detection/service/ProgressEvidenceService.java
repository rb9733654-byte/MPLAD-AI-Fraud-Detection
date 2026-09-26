package com.mplad.fraud_detection.service;

import com.mplad.fraud_detection.dto.ProgressEvidenceResponse;
import com.mplad.fraud_detection.entity.ProgressEvidence;
import com.mplad.fraud_detection.entity.Project;
import com.mplad.fraud_detection.entity.ProjectHistory;
import com.mplad.fraud_detection.repository.ProgressEvidenceRepository;
import com.mplad.fraud_detection.repository.ProjectHistoryRepository;
import com.mplad.fraud_detection.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProgressEvidenceService {
    private static final double EARTH_METRES = 6_371_000;
    private static final String ACCEPTED_LOCATION_STATUS = "Location Matched";
    private final ProjectRepository projects;
    private final ProgressEvidenceRepository evidence;
    private final ProjectHistoryRepository history;
    private final Path storage;
    private final BigDecimal radius;

    public ProgressEvidenceService(ProjectRepository projects, ProgressEvidenceRepository evidence,
            ProjectHistoryRepository history,
            @Value("${mplad.geo.verification-radius-metres:500}") BigDecimal radius,
            @Value("${mplad.upload.directory:uploads/progress-evidence}") String directory) {
        this.projects = projects;
        this.evidence = evidence;
        this.history = history;
        this.radius = radius;
        this.storage = Paths.get(directory).toAbsolutePath().normalize();
    }

    public List<Project> assigned(String contractorId) {
        return projects.findByContractorIdOrderByProjectIdAsc(contractorId);
    }

    @Transactional
    public ProgressEvidenceResponse submit(Long projectId, String contractorId, BigDecimal latitude,
            BigDecimal longitude, BigDecimal completion, BigDecimal expenditure, MultipartFile photo) {
        if (contractorId == null || contractorId.isBlank()) throw new IllegalArgumentException("Contractor is required.");
        Project project = projects.findById(projectId).orElseThrow(() -> new IllegalArgumentException("Project not found."));
        if (!contractorId.equals(project.getContractorId())) throw new SecurityException("This contractor is not assigned to the project.");
        validateUpdate(project, latitude, longitude, completion, expenditure);

        ProgressEvidence item = new ProgressEvidence();
        item.setProject(project);
        item.setContractorId(contractorId);
        item.setCompletionPercentage(completion);
        item.setExpenditure(expenditure);
        item.setSubmittedAt(LocalDateTime.now());
        item.setSubmittedLatitude(latitude);
        item.setSubmittedLongitude(longitude);
        if (photo != null && !photo.isEmpty()) item.setPhotoReference(store(photo));

        String verificationStatus = getVerificationStatus(project, latitude, longitude, item);
        item.setVerificationStatus(verificationStatus);
        // Only a positive proximity match is trusted for the current project record.
        // Mismatched and unverified submissions remain in evidence history for review.
        boolean accepted = ACCEPTED_LOCATION_STATUS.equals(verificationStatus);
        item.setAcceptedForProjectState(accepted);
        if (accepted) {
            savePreviousState(project);
            if (completion != null) {
                project.setCompletionPercentage(completion);
            }
            BigDecimal currentCompletion = project.getCompletionPercentage();
            project.setIsCompleted(currentCompletion != null && currentCompletion.compareTo(BigDecimal.valueOf(100)) >= 0);
            if (expenditure != null) project.setActualExpenditure(expenditure);
            BigDecimal currentExpenditure = project.getActualExpenditure();
            if (currentCompletion != null && currentExpenditure != null)
                project.setExpenditurePerCompletionPercent(currentCompletion.signum() == 0 ? BigDecimal.ZERO
                        : currentExpenditure.divide(currentCompletion, 4, RoundingMode.HALF_UP));
            project.setLastAnalysisScore(null);
            project.setLastAnalysisLevel(null);
            projects.save(project);
        }
        return ProgressEvidenceResponse.from(evidence.save(item), accepted);
    }

    private void savePreviousState(Project project) {
        ProjectHistory snapshot = new ProjectHistory();
        snapshot.setProject(project);
        snapshot.setUpdateDate(LocalDate.now());
        snapshot.setCompletionPercentage(project.getCompletionPercentage());
        snapshot.setExpenditure(project.getActualExpenditure());
        snapshot.setDelayDays(project.getDelayDays() == null ? 0 : project.getDelayDays());
        snapshot.setStatus(project.getCompletionPercentage().compareTo(BigDecimal.valueOf(100)) >= 0
                ? "Completed" : "In Progress");
        snapshot.setIndicatorScore(project.getLastAnalysisScore());
        snapshot.setIndicatorLevel(project.getLastAnalysisLevel());
        history.save(snapshot);
    }

    public List<ProgressEvidenceResponse> forProject(Long projectId) {
        if (!projects.existsById(projectId)) throw new IllegalArgumentException("Project not found.");
        return evidence.findByProjectIdOrderBySubmittedAtDescIdDesc(projectId).stream()
                .map(item -> ProgressEvidenceResponse.from(item, item.isAcceptedForProjectState()))
                .toList();
    }

    private void validateUpdate(Project project, BigDecimal latitude, BigDecimal longitude,
            BigDecimal completion, BigDecimal expenditure) {
        validateCoordinates(latitude, longitude);
        if (completion == null && expenditure == null) throw new IllegalArgumentException("Provide completion or expenditure for this update.");
        if (completion != null && (completion.compareTo(BigDecimal.ZERO) < 0 || completion.compareTo(BigDecimal.valueOf(100)) > 0))
            throw new IllegalArgumentException("Completion must be from 0 to 100 percent.");
        if (expenditure != null && (expenditure.compareTo(BigDecimal.ZERO) < 0
                || project.getReleasedAmount() == null || expenditure.compareTo(project.getReleasedAmount()) > 0))
            throw new IllegalArgumentException("Expenditure must be non-negative and cannot exceed funds released for this project.");
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) throw new IllegalArgumentException("Both latitude and longitude are required.");
        if (latitude != null && (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0))
            throw new IllegalArgumentException("Invalid GPS coordinates.");
    }

    private String getVerificationStatus(Project project, BigDecimal latitude, BigDecimal longitude, ProgressEvidence item) {
        if (latitude == null || project.getLatitude() == null || project.getLongitude() == null) return "Verification Required";
        double distance = distance(latitude.doubleValue(), longitude.doubleValue(), project.getLatitude().doubleValue(), project.getLongitude().doubleValue());
        item.setDistanceMetres(BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP));
        return distance <= radius.doubleValue() ? "Location Matched" : "Location Mismatch — Review Required";
    }

    private String store(MultipartFile file) {
        String type = file.getContentType();
        if (type == null || !List.of("image/jpeg", "image/png", "image/webp").contains(type) || file.getSize() > 5 * 1024 * 1024)
            throw new IllegalArgumentException("Upload a JPG, PNG or WebP image up to 5 MB.");
        String extension = switch (type) { case "image/png" -> ".png"; case "image/webp" -> ".webp"; default -> ".jpg"; };
        try {
            Files.createDirectories(storage);
            String name = UUID.randomUUID() + extension;
            Files.copy(file.getInputStream(), storage.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            return name;
        } catch (IOException exception) {
            throw new IllegalStateException("Photo upload could not be completed.");
        }
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_METRES * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
