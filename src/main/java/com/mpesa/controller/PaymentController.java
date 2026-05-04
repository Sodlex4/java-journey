package com.mpesa.controller;

import com.mpesa.dto.ApiResponse;
import com.mpesa.dto.DepositRequest;
import com.mpesa.dto.WithdrawRequest;
import com.mpesa.dto.TransferRequest;
import com.mpesa.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final UserService userService;

    public PaymentController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@Valid @RequestBody DepositRequest request) {
        String result = userService.deposit(request.getUserId(), request.getAmount());
        return ResponseEntity.ok(ApiResponse.success(null, result));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@Valid @RequestBody WithdrawRequest request) {
        if (!userService.verifyPin(request.getUserId(), request.getPin())) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid PIN"));
        }

        String result = userService.withdraw(request.getUserId(), request.getAmount());
        return ResponseEntity.ok(ApiResponse.success(null, result));
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@Valid @RequestBody TransferRequest request) {
        if (!userService.verifyPin(request.getFromUserId(), request.getPin())) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid PIN"));
        }

        String result = userService.transfer(request.getFromUserId(), request.getToUserId(), request.getAmount());
        return ResponseEntity.ok(ApiResponse.success(null, result));
    }
}