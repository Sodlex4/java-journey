package com.mpesa.controller;

import com.mpesa.dto.ApiResponse;
import com.mpesa.dto.ChangePinRequest;
import com.mpesa.dto.LoginRequest;
import com.mpesa.dto.RegisterRequest;
import com.mpesa.exception.PaymentException;
import com.mpesa.service.UserService;
import com.mpesa.model.User;
import com.mpesa.model.Transaction;
import com.mpesa.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
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
        User user = userService.createUser(request.getUsername(), request.getPin(), request.getBalance());

        return ResponseEntity.ok(ApiResponse.success(
            java.util.Map.of("userId", user.getId(), "username", user.getUsername()),
            "User registered successfully"
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        if (userService.isLocked(request.getUserId())) {
            long remaining = userService.getLockRemainingSeconds(request.getUserId());
            throw new PaymentException("Account locked. Try again in " + remaining + " seconds.");
        }

        if (userService.verifyPin(request.getUserId(), request.getPin())) {
            String token = jwtService.generateToken(request.getUserId());
            return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of("token", token, "userId", request.getUserId()),
                "Login successful"
            ));
        }

        throw new PaymentException("Invalid PIN");
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

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@AuthenticationPrincipal Integer userId) {
        BigDecimal balance = userService.getBalance(userId);
        if (balance == null) {
            return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of("balance", BigDecimal.ZERO),
                null
            ));
        }
        return ResponseEntity.ok(ApiResponse.success(
            java.util.Map.of("balance", balance),
            null
        ));
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(@AuthenticationPrincipal Integer userId) {
        List<Transaction> transactions = userService.getTransactionHistory(userId);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/change-pin")
    public ResponseEntity<?> changePin(
            @AuthenticationPrincipal Integer userId,
            @Valid @RequestBody ChangePinRequest request) {
        if (!userId.equals(request.getUserId())) {
            throw new PaymentException("Cannot change another user's PIN");
        }
        if (userService.changePin(userId, request.getCurrentPin(), request.getNewPin())) {
            return ResponseEntity.ok(ApiResponse.success("PIN changed successfully"));
        }
        throw new PaymentException("Current PIN is incorrect");
    }
}
