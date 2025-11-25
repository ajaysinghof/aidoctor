package com.aidoctor.controller;

import com.aidoctor.model.AppUser;
import com.aidoctor.repository.UserRepository;
import com.aidoctor.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    // -------------------------
    // LOGIN
    // -------------------------
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {

        String username = request.get("username");
        String password = request.get("password");

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtService.generateToken(username);

        return Map.of(
                "status", "success",
                "token", token,
                "username", user.getUsername(),
                "role", user.getRole()
        );
    }


    // -------------------------
    // REGISTER USER
    // -------------------------
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> request) {

        String username = request.get("username");
        String password = request.get("password");

        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("USER");

        userRepository.save(user);

        return Map.of(
                "status", "success",
                "message", "User registered successfully"
        );
    }
}
