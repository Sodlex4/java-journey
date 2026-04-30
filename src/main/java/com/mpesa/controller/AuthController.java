package com.mpesa.controller;

import com.mpesa.dto.ApiResponse;
import com.mpesa.dto.LoginRequest;
import com.mpesa.dto.RegisterRequest;
import com.mpesa.service.UserService;
import com.mpesa.model.User;
import com.mpesa.model.Transaction;
import com.mpesa.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.createUser(request.getUsername(), request.getBalance());

            return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of("userId", user.getId(), "username", user.getUsername()),
                "User registered successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        if (userService.isLocked(request.getUserId())) {
            long remaining = userService.getLockRemainingSeconds(request.getUserId());
            return ResponseEntity.status(423).body(ApiResponse.error(
                "Account locked. Try again in " + remaining + " seconds."
            ));
        }

        if (userService.verifyPin(request.getUserId(), request.getPin())) {
            String token = jwtService.generateToken(request.getUserId());
            return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of("token", token, "userId", request.getUserId()),
                "Login successful"
            ));
        }

        return ResponseEntity.status(401).body(ApiResponse.error("Invalid PIN"));
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<?> getUser(@PathVariable Integer id) {
        return userService.findById(id)
            .map(user -> ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of("id", user.getId(), "username", user.getUsername(), "balance", user.getBalance()),
                null
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/balance/{userId}")
    public ResponseEntity<?> getBalance(@PathVariable Integer userId) {
        Double balance = userService.getBalance(userId);
        if (balance == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(
            java.util.Map.of("balance", balance),
            null
        ));
    }

    @GetMapping("/transactions/{userId}")
    public ResponseEntity<?> getTransactions(@PathVariable Integer userId) {
        List<Transaction> transactions = userService.getTransactionHistory(userId);
        return ResponseEntity.ok(transactions);
    }
}