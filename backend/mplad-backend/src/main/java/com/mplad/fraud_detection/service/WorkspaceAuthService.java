package com.mplad.fraud_detection.service;

import com.mplad.fraud_detection.dto.WorkspaceUserResponse;
import com.mplad.fraud_detection.entity.WorkspaceUser;
import com.mplad.fraud_detection.repository.ProjectRepository;
import com.mplad.fraud_detection.repository.WorkspaceUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class WorkspaceAuthService {
    public static final String ROLE = "workspaceRole";
    public static final String IDENTITY = "workspaceIdentity";
    public static final String CSRF = "workspaceCsrf";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final WorkspaceUserRepository users;
    private final ProjectRepository projects;
    private final PasswordEncoder passwords;

    public WorkspaceAuthService(WorkspaceUserRepository users, ProjectRepository projects, PasswordEncoder passwords) {
        this.users = users; this.projects = projects; this.passwords = passwords;
    }

    @Transactional
    public void register(String role, String identityInput, String usernameInput, String password, String confirmation) {
        String identity = normalize(role, identityInput);
        validatePassword(password);
        if (!password.equals(confirmation)) throw new IllegalArgumentException("Passwords do not match.");
        String username = usernameInput == null || usernameInput.isBlank()
                ? ("CONTRACTOR".equals(role) ? identity : "member_" + UUID.randomUUID().toString().replace("-", ""))
                : usernameInput.trim();
        if (!username.matches("[A-Za-z0-9_.-]{3,50}")) throw new IllegalArgumentException("Username must be 3 to 50 letters, numbers, dots, dashes or underscores.");
        if ("CONTRACTOR".equals(role) && !projects.existsByContractorId(identity))
            throw new IllegalArgumentException("No projects are assigned to this contractor ID.");
        if (users.existsByRoleAndIdentity(role, identity)) throw new IllegalArgumentException("This workspace account already exists.");
        if (users.existsByRoleAndUsername(role, username)) throw new IllegalArgumentException("This username is already in use.");
        WorkspaceUser user = new WorkspaceUser(); user.setRole(role); user.setUsername(username); user.setIdentity(identity); user.setPasswordHash(passwords.encode(password));
        users.save(user);
    }

    public WorkspaceUserResponse login(String role, String identityInput, String password, HttpServletRequest request) {
        String identity = normalize(role, identityInput);
        WorkspaceUser user = users.findByRoleAndIdentity(role, identity).orElseThrow(() -> new IllegalArgumentException("Account or password is incorrect."));
        if (!passwords.matches(password == null ? "" : password, user.getPasswordHash())) throw new IllegalArgumentException("Account or password is incorrect.");
        if ("CONTRACTOR".equals(role) && !projects.existsByContractorId(identity))
            throw new IllegalArgumentException("No projects are currently assigned to this contractor ID.");
        return openSession(user, request);
    }

    public WorkspaceUserResponse current(String role, HttpSession session) {
        Object actualRole = session.getAttribute(ROLE), identity = session.getAttribute(IDENTITY), csrf = session.getAttribute(CSRF);
        if (!role.equals(actualRole) || !(identity instanceof String value) || !(csrf instanceof String token))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
        return new WorkspaceUserResponse(role, value, token);
    }

    public void logout(HttpServletRequest request, String csrf) {
        HttpSession session = request.getSession(false);
        if (session == null) return;
        Object expected = session.getAttribute(CSRF);
        if (!(expected instanceof String token) || csrf == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), csrf.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Request verification failed.");
        session.invalidate();
    }

    public static void requireRole(HttpSession session, String expectedRole) {
        if (!expectedRole.equals(session.getAttribute(ROLE))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
    }

    public static void requireCsrf(HttpSession session, String provided) {
        Object expected = session.getAttribute(CSRF);
        if (!(expected instanceof String token) || provided == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Request verification failed. Refresh and try again.");
    }

    private WorkspaceUserResponse openSession(WorkspaceUser user, HttpServletRequest request) {
        HttpSession old = request.getSession(false); if (old != null) old.invalidate();
        HttpSession session = request.getSession(true); byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String csrf = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(ROLE, user.getRole()); session.setAttribute(IDENTITY, user.getIdentity()); session.setAttribute(CSRF, csrf);
        return new WorkspaceUserResponse(user.getRole(), user.getIdentity(), csrf);
    }
    private static String normalize(String role, String value) {
        if (!"AUTHORITY".equals(role) && !"CONTRACTOR".equals(role)) throw new IllegalArgumentException("Unsupported workspace role.");
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Email or contractor ID is required.");
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("AUTHORITY".equals(role)) {
            normalized = value.trim().toLowerCase(Locale.ROOT);
            if (normalized.length() > 254 || !normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("Enter a valid email address.");
        } else if (!normalized.matches("[A-Z0-9_-]{3,50}")) throw new IllegalArgumentException("Enter a valid contractor ID.");
        return normalized;
    }
    private static void validatePassword(String value) {
        if (value == null || value.length() < 8 || value.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Password must be at least 8 characters and no more than 72 UTF-8 bytes.");
    }
}
