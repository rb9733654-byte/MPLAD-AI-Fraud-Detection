package com.mplad.fraud_detection.controller;

import com.mplad.fraud_detection.dto.PublicFeedbackRequest;
import com.mplad.fraud_detection.dto.PublicFeedbackResponse;
import com.mplad.fraud_detection.dto.PublicProjectResponse;
import com.mplad.fraud_detection.entity.Project;
import com.mplad.fraud_detection.entity.PublicFeedback;
import com.mplad.fraud_detection.entity.PublicUser;
import com.mplad.fraud_detection.repository.ProjectRepository;
import com.mplad.fraud_detection.repository.PublicFeedbackRepository;
import com.mplad.fraud_detection.repository.PublicUserRepository;
import com.mplad.fraud_detection.service.PublicAuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RestController
@RequestMapping("/api/public/projects")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:5502", "http://127.0.0.1:5502"}, allowCredentials = "true")
public class PublicProjectController {
    private final ProjectRepository projects;
    private final PublicFeedbackRepository feedback;
    private final PublicUserRepository users;
    public PublicProjectController(ProjectRepository projects, PublicFeedbackRepository feedback, PublicUserRepository users) {
        this.projects = projects; this.feedback = feedback; this.users = users;
    }

    @GetMapping
    public List<PublicProjectResponse> list(@RequestParam(required = false) String search,
                                            @RequestParam(required = false) String state,
                                            @RequestParam(required = false) String district,
                                            @RequestParam(required = false) String constituency,
                                            @RequestParam(required = false) String projectType) {
        return projects.findAll().stream().filter(p -> matches(search, p.getProjectId(), p.getProjectType(), p.getDistrict(), p.getConstituency(), p.getState()))
                .filter(p -> matches(state, p.getState())).filter(p -> matches(district, p.getDistrict()))
                .filter(p -> matches(constituency, p.getConstituency())).filter(p -> matches(projectType, p.getProjectType()))
                .map(PublicProjectResponse::from).toList();
    }
    @GetMapping("/{id}")
    public PublicProjectResponse detail(@PathVariable Long id) { return PublicProjectResponse.from(findProject(id)); }
    @GetMapping("/{id}/feedback")
    public List<PublicFeedbackResponse> listFeedback(@PathVariable Long id) {
        findProject(id); return feedback.findByProjectIdOrderByIdDesc(id).stream().map(PublicFeedbackResponse::from).toList();
    }
    @PostMapping("/{id}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicFeedbackResponse submitFeedback(@PathVariable Long id, @RequestBody PublicFeedbackRequest request,
                                                  HttpSession session, @RequestHeader(value = "X-CSRF-Token", required = false) String csrf) {
        Object userIdValue = session.getAttribute(PublicAuthService.SESSION_USER_ID);
        if (!(userIdValue instanceof Long userId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in to submit feedback.");
        Object expected = session.getAttribute(PublicAuthService.SESSION_CSRF);
        if (!(expected instanceof String token) || csrf == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), csrf.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Request verification failed. Refresh and try again.");
        if (request == null || request.rating() == null || request.rating() < 1 || request.rating() > 5)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be a whole number from 1 to 5.");
        String comment = request.comment() == null ? "" : request.comment().trim();
        if (comment.isBlank() || comment.length() > 1000 || comment.codePoints().anyMatch(Character::isISOControl))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment must contain 1 to 1000 readable characters.");
        Project project = findProject(id);
        PublicUser user = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in."));
        PublicFeedback item = new PublicFeedback(); item.setProject(project); item.setPublicUser(user); item.setRating(request.rating()); item.setComment(comment);
        return PublicFeedbackResponse.from(feedback.save(item));
    }
    private Project findProject(Long id) { return projects.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found.")); }
    private static boolean matches(String filter, String... values) {
        if (filter == null || filter.isBlank()) return true;
        String needle = filter.trim().toLowerCase(java.util.Locale.ROOT);
        return java.util.Arrays.stream(values).anyMatch(v -> v != null && v.toLowerCase(java.util.Locale.ROOT).contains(needle));
    }
}
