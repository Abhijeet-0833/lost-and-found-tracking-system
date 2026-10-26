package com.lostfound.controller;

import com.lostfound.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.lostfound.util.JwtUtil;
import org.springframework.http.ResponseEntity;

import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${admin.email:admin@example.com}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    // 1. Send OTP to User
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestParam String email) {
        String result = authService.sendOtp(email);
        if ("OTP sent successfully!".equals(result)) {
            return ResponseEntity.ok(Map.of("message", result));
        }
        return ResponseEntity.badRequest().body(Map.of("message", result));
    }

    @Autowired
    private com.lostfound.repository.UserRepository userRepository;

    // 3. Admin Login (Password)
    @PostMapping("/admin-login")
    public ResponseEntity<?> adminLogin(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        // Admin login is disabled unless an administrator password is configured.
        if (adminPassword != null && !adminPassword.isBlank()
                && email != null && password != null
                && adminEmail.equalsIgnoreCase(email) && adminPassword.equals(password)) {

            // Ensure admin exists in DB with ADMIN role
            com.lostfound.model.User admin = userRepository.findByEmail(email).orElseGet(() -> {
                com.lostfound.model.User newAdmin = new com.lostfound.model.User();
                newAdmin.setEmail(email);
                newAdmin.setUsername("Admin");
                newAdmin.setRole("ADMIN");
                return newAdmin;
            });
            // Ensure role is ADMIN (in case they previously registered as a USER)
            admin.setRole("ADMIN");
            userRepository.save(admin);

            String token = jwtUtil.generateToken(email);
            return ResponseEntity.ok(Map.of("token", token));
        }
        return ResponseEntity.status(401).body("Invalid Admin Credentials");
    }

    // 2. Verify OTP and Generate Token
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        if (authService.verifyOtp(email, otp)) {
            String token = jwtUtil.generateToken(email);
            return ResponseEntity.ok(Map.of("token", token));
        }
        return ResponseEntity.status(401).body("Invalid OTP or expired");
    }
}
