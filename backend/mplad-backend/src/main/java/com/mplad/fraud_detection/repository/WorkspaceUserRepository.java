package com.mplad.fraud_detection.repository;

import com.mplad.fraud_detection.entity.WorkspaceUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WorkspaceUserRepository extends JpaRepository<WorkspaceUser, Long> {
    Optional<WorkspaceUser> findByRoleAndIdentity(String role, String identity);
    boolean existsByRoleAndIdentity(String role, String identity);
    boolean existsByRoleAndUsername(String role, String username);
}
