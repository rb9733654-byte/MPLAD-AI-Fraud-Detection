package com.mplad.fraud_detection.service;

import com.mplad.fraud_detection.dto.PublicUserResponse;
import com.mplad.fraud_detection.dto.DemoOtpResponse;
import com.mplad.fraud_detection.entity.PublicOtpVerification;
import com.mplad.fraud_detection.entity.PublicUser;
import com.mplad.fraud_detection.repository.PublicOtpRepository;
import com.mplad.fraud_detection.repository.PublicUserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class PublicAuthService {
    public static final String SESSION_USER_ID = "publicUserId";
    public static final String SESSION_CSRF = "publicCsrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ATTEMPTS = 5;
    private static final int OTP_LIFETIME_MINUTES = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private final PublicUserRepository users;
    private final PublicOtpRepository otps;
    private final PasswordEncoder encoder;
    private final boolean demoMode;

    public PublicAuthService(PublicUserRepository users, PublicOtpRepository otps, PasswordEncoder encoder,
                             @Value("${app.otp.demo-mode:false}") boolean demoMode) {
        this.users = users; this.otps = otps; this.encoder = encoder; this.demoMode = demoMode;
    }

    @Transactional
    public DemoOtpResponse register(String emailInput, String password, String confirmPassword) {
        String email = normalizeEmail(emailInput);
        validatePassword(password);
        if (!password.equals(confirmPassword)) throw new IllegalArgumentException("Passwords do not match.");
        PublicUser existing = users.findByEmail(email).orElse(null);
        if (existing != null) {
            if (existing.isEmailVerified() || "ACTIVE".equals(existing.getAccountStatus()))
                throw new IllegalArgumentException("An account with this email already exists. Please log in.");
            return resend(email);
        }
        if (!demoMode) throw new IllegalStateException("Demo OTP mode is disabled; registration is unavailable without a configured verification delivery method.");
        PublicUser user = new PublicUser(); user.setUsername("member_" + UUID.randomUUID().toString().replace("-", ""));
        user.setEmail(email); user.setPasswordHash(encoder.encode(password));
        user.setEmailVerified(false); user.setAccountStatus("PENDING"); user = users.saveAndFlush(user);
        return createOtp(user);
    }

    @Transactional
    public DemoOtpResponse resend(String emailInput) {
        String email = normalizeEmail(emailInput);
        PublicUser user = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("No pending registration was found for this email."));
        if (user.isEmailVerified()) throw new IllegalArgumentException("This account is already verified. Please log in.");
        PublicOtpVerification latest = otps.findTopByPublicUserIdAndVerifiedFalseOrderByIdDesc(user.getId()).orElse(null);
        if (latest != null && latest.getExpiresAt().minusMinutes(OTP_LIFETIME_MINUTES).plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Please wait before requesting another code.");
        if (!demoMode) throw new IllegalStateException("Demo OTP mode is disabled.");
        return createOtp(user);
    }

    @Transactional(noRollbackFor = InvalidOtpException.class)
    public void verify(String emailInput, String otp) {
        String email = normalizeEmail(emailInput);
        if (otp == null || !otp.matches("\\d{6}")) throw new IllegalArgumentException("Enter the 6-digit verification code.");
        PublicUser user = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("No pending registration was found."));
        if (user.isEmailVerified()) throw new IllegalArgumentException("This account is already verified. Please log in.");
        PublicOtpVerification verification = otps.findTopByPublicUserIdAndVerifiedFalseOrderByIdDesc(user.getId()).orElseThrow(() -> new IllegalArgumentException("No active verification code. Request a new code."));
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) throw new IllegalArgumentException("The verification code expired. Request a new code.");
        if (verification.getAttempts() >= MAX_ATTEMPTS) throw new IllegalArgumentException("Too many attempts. Request a new code.");
        verification.setAttempts(verification.getAttempts() + 1);
        if (!encoder.matches(otp, verification.getOtpHash())) {
            otps.save(verification);
            throw new InvalidOtpException("The verification code is incorrect.");
        }
        verification.setVerified(true); user.setEmailVerified(true); user.setAccountStatus("ACTIVE");
        otps.save(verification); users.save(user);
    }

    public PublicUser authenticate(String emailInput, String password) {
        String email = normalizeEmail(emailInput);
        PublicUser user = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Email or password is incorrect."));
        if (!encoder.matches(password == null ? "" : password, user.getPasswordHash())) throw new IllegalArgumentException("Email or password is incorrect.");
        if (!user.isEmailVerified() || !"ACTIVE".equals(user.getAccountStatus())) throw new IllegalArgumentException("Verify your account before logging in.");
        return user;
    }

    public PublicUserResponse startSession(PublicUser user, HttpServletRequest request) {
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) oldSession.invalidate();
        HttpSession session = request.getSession(true);
        String csrf = randomToken(); session.setAttribute(SESSION_USER_ID, user.getId()); session.setAttribute(SESSION_CSRF, csrf);
        return new PublicUserResponse(user.getId(), user.getEmail(), csrf);
    }

    public PublicUserResponse currentUser(HttpSession session) {
        Object id = session.getAttribute(SESSION_USER_ID);
        if (!(id instanceof Long userId)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Please log in.");
        PublicUser user = users.findById(userId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Please log in."));
        return new PublicUserResponse(user.getId(), user.getEmail(), (String) session.getAttribute(SESSION_CSRF));
    }

    private DemoOtpResponse createOtp(PublicUser user) {
        int number = RANDOM.nextInt(1_000_000); String otp = String.format(Locale.ROOT, "%06d", number);
        PublicOtpVerification record = new PublicOtpVerification(); record.setPublicUser(user); record.setEmail(user.getEmail());
        record.setOtpHash(encoder.encode(otp)); record.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_LIFETIME_MINUTES));
        record.setAttempts(0); record.setVerified(false); otps.save(record);
        return new DemoOtpResponse("Demo verification code generated. Use this code to verify your account.", otp);
    }

    private static String normalizeEmail(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Email is required.");
        String email = value.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 254 || !email.matches("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$".toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Enter a valid email address.");
        return email;
    }
    private static void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) throw new IllegalArgumentException("Password must be at least 8 characters and no more than 72 UTF-8 bytes.");
    }
    private static String randomToken() { byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    public static class InvalidOtpException extends RuntimeException { public InvalidOtpException(String message) { super(message); } }
}
