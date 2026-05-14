package com.mpesa.controller;

import com.mpesa.dto.ApiResponse;
import com.mpesa.dto.ChangePinRequest;
import com.mpesa.dto.ErrorCode;
import com.mpesa.dto.LoginRequest;
import com.mpesa.dto.RegisterRequest;
import com.mpesa.exception.PaymentException;
import com.mpesa.service.UserService;
import com.mpesa.model.User;
import com.mpesa.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

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
        if (userService.verifyPin(request.getUserId(), request.getPin())) {
            String token = jwtService.generateToken(request.getUserId());
            return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of("token", token, "userId", request.getUserId()),
                "Login successful"
            ));
        }

        return ResponseEntity.status(401).body(
            ApiResponse.error("Invalid credentials", ErrorCode.INVALID_CREDENTIALS));
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
    public ResponseEntity<?> getTransactions(
            @AuthenticationPrincipal Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<com.mpesa.model.Transaction> txPage = userService.getTransactionHistory(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(java.util.Map.of(
            "transactions", txPage.getContent(),
            "page", txPage.getNumber(),
            "size", txPage.getSize(),
            "totalElements", txPage.getTotalElements(),
            "totalPages", txPage.getTotalPages()
        ));
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
