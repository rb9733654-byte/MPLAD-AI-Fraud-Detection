package com.mplad.fraud_detection.service;

import com.mplad.fraud_detection.dto.UserLoginDto;
import com.mplad.fraud_detection.entity.User;
import com.mplad.fraud_detection.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> authenticateUser(UserLoginDto loginDto) {
        if (loginDto == null) {
            return Optional.empty();
        }

        String username = loginDto.getUsername() == null ? "" : loginDto.getUsername().trim();
        String email = loginDto.getEmail() == null ? "" : loginDto.getEmail().trim();
        String password = loginDto.getPassword() == null ? "" : loginDto.getPassword();

        if ((username.isBlank() && email.isBlank()) || password.isBlank()) {
            return Optional.empty();
        }

        Optional<User> userMatch = Optional.empty();

        if (!username.isBlank()) {
            userMatch = userRepository.findByUsername(username);
        }

        if (userMatch.isEmpty() && !email.isBlank()) {
            userMatch = userRepository.findByEmail(email);
        }

        return userMatch.filter(user -> user.getPassword() != null && user.getPassword().equals(password));
    }

    public boolean updateRole(UserLoginDto roleUpdateDto) {
        if (roleUpdateDto == null || roleUpdateDto.getRole() == null) {
            return false;
        }

        String username = roleUpdateDto.getUsername() == null ? "" : roleUpdateDto.getUsername().trim();
        String email = roleUpdateDto.getEmail() == null ? "" : roleUpdateDto.getEmail().trim();
        String role = roleUpdateDto.getRole().trim();

        if (username.isBlank() && email.isBlank()) {
            return false;
        }
        if (!role.equals("Authority") && !role.equals("District") && !role.equals("Public")) {
            return false;
        }

        Optional<User> userMatch = username.isBlank()
                ? userRepository.findByEmail(email)
                : userRepository.findByUsername(username);

        if (userMatch.isEmpty() && !email.isBlank()) {
            userMatch = userRepository.findByEmail(email);
        }
        if (userMatch.isEmpty()) {
            return false;
        }

        User user = userMatch.get();
        String currentRole = user.getRole() == null ? "" : user.getRole().trim();
        if (isAssignedRole(currentRole) && !currentRole.equals(role)) {
            return false;
        }

        user.setRole(role);
        userRepository.save(user);
        return true;
    }

    private boolean isAssignedRole(String role) {
        return role.equals("Authority") || role.equals("District") || role.equals("Public");
    }

    public String registerUser(UserLoginDto registrationDto) {
        if (registrationDto == null) {
            return "Please provide all required details.";
        }

        String username = registrationDto.getUsername() == null ? "" : registrationDto.getUsername().trim();
        String email = registrationDto.getEmail() == null ? "" : registrationDto.getEmail().trim();
        String password = registrationDto.getPassword() == null ? "" : registrationDto.getPassword();

        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            return "Username, email, and password are required.";
        }
        if (userRepository.existsByUsername(username)) {
            return "Username already exists.";
        }
        if (userRepository.existsByEmail(email)) {
            return "Email already exists.";
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole("PENDING");
        userRepository.save(user);
        return null;
    }
}
