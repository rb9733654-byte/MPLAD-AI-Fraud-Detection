package com.mplad.fraud_detection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mplad.fraud_detection.dto.DemoOtpResponse;
import com.mplad.fraud_detection.entity.PublicOtpVerification;
import com.mplad.fraud_detection.entity.PublicUser;
import com.mplad.fraud_detection.repository.PublicOtpRepository;
import com.mplad.fraud_detection.repository.PublicUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PublicAuthServiceTests {
    @Mock private PublicUserRepository users;
    @Mock private PublicOtpRepository otps;

    @Test void registrationStoresHashesAndReturnsDemoOtp() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        when(users.findByEmail("demo.user@example.test")).thenReturn(Optional.empty());
        when(users.saveAndFlush(any(PublicUser.class))).thenAnswer(invocation -> {
            PublicUser user = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 17L);
            return user;
        });
        when(otps.save(any(PublicOtpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PublicAuthService service = new PublicAuthService(users, otps, encoder, true);

        ch.qos.logback.classic.Logger otpLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(PublicAuthService.class);
        ch.qos.logback.classic.Level previousLevel = otpLogger.getLevel();
        otpLogger.setLevel(ch.qos.logback.classic.Level.OFF);
        DemoOtpResponse response;
        try { response = service.register("Demo.User@example.test", "correct-horse-1", "correct-horse-1"); }
        finally { otpLogger.setLevel(previousLevel); }

        assertThat(response.message()).contains("Demo verification code generated");
        assertThat(response.demoCode()).matches("\\b\\d{6}\\b");
        assertThat(response.message()).doesNotContain(response.demoCode());
        ArgumentCaptor<PublicUser> userCaptor = ArgumentCaptor.forClass(PublicUser.class);
        verify(users).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).startsWith("$2");
        assertThat(encoder.matches("correct-horse-1", userCaptor.getValue().getPasswordHash())).isTrue();
        ArgumentCaptor<PublicOtpVerification> otpCaptor = ArgumentCaptor.forClass(PublicOtpVerification.class);
        verify(otps).save(otpCaptor.capture());
        assertThat(otpCaptor.getValue().getOtpHash()).startsWith("$2");
        assertThat(otpCaptor.getValue().getOtpHash()).doesNotContain(response.demoCode());
        assertThat(encoder.matches(response.demoCode(), otpCaptor.getValue().getOtpHash())).isTrue();
        assertThat(otpCaptor.getValue().getExpiresAt()).isNotNull();
    }
}
