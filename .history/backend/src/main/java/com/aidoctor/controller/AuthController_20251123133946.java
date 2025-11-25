package com.aidoctor.controller;

import com.aidoctor.model.AppUser;
import com.aidoctor.repository.UserRepository;
import com.aidoctor.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public Map<String,Object> register(@RequestBody Map<String,String> body){
        String username = body.get("username");
        String password = body.get("password");
        if(username==null || password==null) return Map.of("error","username and password required");

        if(userRepository.findByUsername(username).isPresent()){
            return Map.of("error","username exists");
        }
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));
        userRepository.save(u);
        String token = jwtUtil.generateToken(username);
        return Map.of("token", token, "username", username);
    }

    @PostMapping("/login")
    public Map<String,Object> login(@RequestBody Map<String,String> body){
        String username = body.get("username");
        String password = body.get("password");
        var userOpt = userRepository.findByUsername(username);
        if(userOpt.isEmpty()) return Map.of("error","invalid");
        AppUser u = userOpt.get();
        if(!encoder.matches(password, u.getPasswordHash())) return Map.of("error","invalid");
        String token = jwtUtil.generateToken(username);
        return Map.of("token", token, "username", username);
    }
}
