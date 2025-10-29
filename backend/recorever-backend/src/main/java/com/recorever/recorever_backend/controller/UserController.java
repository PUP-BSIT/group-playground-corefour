package com.recorever.recorever_backend.controller;

import com.recorever.recorever_backend.config.JwtUtil;
import com.recorever.recorever_backend.model.User;
import com.recorever.recorever_backend.service.UserService;
import com.recorever.recorever_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService service;

    @Autowired
    private UserRepository repo;

    @Autowired
    private JwtUtil jwtUtil;


    @PostMapping("/register-user")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");

        Map<String, Object> result = service.register(name, email, password);
        if (result.containsKey("error")) {
            return ResponseEntity.badRequest().body(result.get("error"));
        }
        return ResponseEntity.status(201).body(result);
    }

    @PostMapping("/login-user")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        Map<String, Object> result = service.login(email, password);

        if (result.containsKey("error")) {
            return ResponseEntity.status(401).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/get-user-data")
    public ResponseEntity<?> getUser(@RequestParam int id) {
        User user = repo.findById(id);
        if (user == null)
            return ResponseEntity.status(404).body("User not found");
        return ResponseEntity.ok(user);
    }

    @PutMapping("/update-user-data")
    public ResponseEntity<?> updateUser(@RequestParam int id,
                                        @RequestParam(required = false) String name,
                                        @RequestParam(required = false) String phone_number,
                                        @RequestParam(required = false) String profile_picture) {

        User user = repo.findById(id);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        // Only update if provided
        if (name != null && !name.isEmpty()) {
            user.setName(name);
        }
        if (phone_number != null && !phone_number.isEmpty()) {
            user.setPhone_number(phone_number);
        }
        if (profile_picture != null && !profile_picture.isEmpty()) {
            user.setProfile_picture(profile_picture);
        }

        boolean updated = repo.updateUser(user.getUser_id(), user.getName(), user.getPhone_number(), user.getProfile_picture());
        if (!updated) {
            return ResponseEntity.badRequest().body("Failed to update user.");
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Profile updated successfully."));
    }

    @DeleteMapping("/delete-user")
    public ResponseEntity<?> deleteUser(@RequestParam int id) {
        boolean deleted = repo.deleteUser(id);
        if (!deleted)
            return ResponseEntity.status(404).body("User not found.");
        return ResponseEntity.ok(Map.of("success", true, "message", "User account deactivated."));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refresh(@RequestParam String refreshToken) {
        User user = repo.findByRefreshToken(refreshToken);
        if (user == null || user.getRefresh_token_expiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(401).body(Map.of("error_message", "Invalid or expired refresh token"));
        }

        // Generate new access token
        String accessToken = jwtUtil.generateToken(user.getUser_id(), user.getName());

        return ResponseEntity.ok(Map.of(
            "access_token", accessToken,
            "token_type", "Bearer",
            "expires_in", 3600,
            "user_id", user.getUser_id(),
            "user_name", user.getName()
        ));
    }
}
