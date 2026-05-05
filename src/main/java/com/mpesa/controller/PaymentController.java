package com.mpesa.controller;

import com.mpesa.dto.ApiResponse;
import com.mpesa.dto.DepositRequest;
import com.mpesa.dto.WithdrawRequest;
import com.mpesa.dto.TransferRequest;
import com.mpesa.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final UserService userService;

    public PaymentController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(
            @AuthenticationPrincipal Integer userId,
            @Valid @RequestBody DepositRequest request) {
        String result = userService.deposit(userId, request.getAmount());
        return ResponseEntity.ok(ApiResponse.success(null, result));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(
            @AuthenticationPrincipal Integer userId,
            @Valid @RequestBody WithdrawRequest request) {
        if (!userService.verifyPin(userId, request.getPin())) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid PIN"));
        }

        String result = userService.withdraw(userId, request.getAmount());
        return ResponseEntity.ok(ApiResponse.success(null, result));
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(
            @AuthenticationPrincipal Integer userId,
            @Valid @RequestBody TransferRequest request) {
        if (!userService.verifyPin(userId, request.getPin())) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid PIN"));
        }

        String result = userService.transfer(userId, request.getToUserId(), request.getAmount());
        return ResponseEntity.ok(ApiResponse.success(null, result));
    }
}
