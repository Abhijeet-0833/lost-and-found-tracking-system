package com.lostfound.service;

import com.lostfound.model.User;
import com.lostfound.repository.UserRepository;
import com.lostfound.util.JwtUtil;
import com.lostfound.util.OtpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpUtil otpUtil;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String sendOtp(String email) {
        String otp = otpUtil.generateOtp();
        
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(email.split("@")[0]);
            newUser.setRole("USER");
            return newUser;
        });

        user.setOtpCode(otp);
        userRepository.save(user);

        if ("BLOCKED".equals(user.getRole())) {
            return "Account is blocked. Please contact support.";
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Your Login OTP");
            message.setText("Your OTP for Campus Tracker is: " + otp);
            mailSender.send(message);
            return "OTP sent successfully!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to send OTP: " + e.getMessage();
        }
    }

    public User validateSession(String token) {
        String email = JwtUtil.extractEmail(token);
        
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public boolean verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOtpCode() != null && user.getOtpCode().equals(otp)) {
            user.setOtpCode(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public String verifyOtpAndLogin(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOtpCode() != null && user.getOtpCode().equals(otp)) {
            user.setOtpCode(null);
            userRepository.save(user);
            return jwtUtil.generateToken(email);
        } else {
            throw new RuntimeException("Invalid OTP");
        }
    }

    public String adminLogin(String username, String rawPassword) {
        User admin = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!"ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("Access Denied: Not an Admin");
        }

        if (passwordEncoder.matches(rawPassword, admin.getPassword())) {
            return jwtUtil.generateToken(admin.getEmail());
        } else {
            throw new RuntimeException("Invalid Password");
        }
    }
}
