package com.mplad.fraud_detection.dto;
import com.mplad.fraud_detection.entity.Project;
public record PublicProjectResponse(Long id, String projectId, String state, String district, String constituency, String projectType, String publicLocation) {
    public static PublicProjectResponse from(Project p) {
        String location = String.join(", ", java.util.stream.Stream.of(p.getConstituency(), p.getDistrict(), p.getState()).filter(v -> v != null && !v.isBlank()).toList());
        return new PublicProjectResponse(p.getId(), p.getProjectId(), p.getState(), p.getDistrict(), p.getConstituency(), p.getProjectType(), location);
    }
}
