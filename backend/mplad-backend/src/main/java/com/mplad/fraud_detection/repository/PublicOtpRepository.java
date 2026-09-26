package com.mplad.fraud_detection.repository;
import com.mplad.fraud_detection.entity.PublicOtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface PublicOtpRepository extends JpaRepository<PublicOtpVerification, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PublicOtpVerification> findTopByPublicUserIdAndVerifiedFalseOrderByIdDesc(Long userId);
}
