package com.mplad.fraud_detection.controller;

import com.mplad.fraud_detection.dto.UserLoginDto;
import com.mplad.fraud_detection.dto.LoginResponse;
import com.mplad.fraud_detection.entity.User;
import com.mplad.fraud_detection.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LoginController {

    private final UserService userService;

    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody UserLoginDto loginDto) {
        Optional<User> authenticatedUser = userService.authenticateUser(loginDto);
        if (authenticatedUser.isPresent()) {
            User user = authenticatedUser.get();
            return ResponseEntity.ok(new LoginResponse(
                    "Login Successful!",
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole()
            ));
        }

        return ResponseEntity.status(401).body("Invalid Username or Password!");
    }

    @PostMapping("/users/role")
    public ResponseEntity<String> updateRole(@RequestBody UserLoginDto roleUpdateDto) {
        if (userService.updateRole(roleUpdateDto)) {
            return ResponseEntity.ok("Access type saved successfully!");
        }

        return ResponseEntity.badRequest().body("Could not save the selected access type.");
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserLoginDto registrationDto) {
        // The old endpoint writes to the retired `users` table and does not
        // preserve the selected role. Keep stale clients out of the DB path.
        return ResponseEntity.status(HttpStatus.GONE)
                .body("This signup page is outdated. Choose your role on Access, then register from Login.");
    }
}
