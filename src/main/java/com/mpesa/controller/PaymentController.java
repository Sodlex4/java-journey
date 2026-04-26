package com.mpesa.controller;

import com.mpesa.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    
    private final UserService userService;
    
    public PaymentController(UserService userService) {
        this.userService = userService;
    }
    
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody Map<String, Object> request) {
        Integer userId = Integer.parseInt(request.get("userId").toString());
        Double amount = Double.parseDouble(request.get("amount").toString());
        
        String result = userService.deposit(userId, amount);
        
        Map<String, Object> response = new HashMap<>();
        if (result.contains("successful")) {
            response.put("success", true);
            response.put("message", result);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("error", result);
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody Map<String, Object> request) {
        Integer userId = Integer.parseInt(request.get("userId").toString());
        Double amount = Double.parseDouble(request.get("amount").toString());
        String pin = (String) request.get("pin");
        
        if (!userService.verifyPin(userId, pin)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Invalid PIN");
            return ResponseEntity.status(401).body(response);
        }
        
        String result = userService.withdraw(userId, amount);
        
        Map<String, Object> response = new HashMap<>();
        if (result.contains("successful")) {
            response.put("success", true);
            response.put("message", result);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("error", result);
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody Map<String, Object> request) {
        Integer fromUserId = Integer.parseInt(request.get("fromUserId").toString());
        Integer toUserId = Integer.parseInt(request.get("toUserId").toString());
        Double amount = Double.parseDouble(request.get("amount").toString());
        String pin = (String) request.get("pin");
        
        if (!userService.verifyPin(fromUserId, pin)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Invalid PIN");
            return ResponseEntity.status(401).body(response);
        }
        
        String result = userService.transfer(fromUserId, toUserId, amount);
        
        Map<String, Object> response = new HashMap<>();
        if (result.contains("successful")) {
            response.put("success", true);
            response.put("message", result);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("error", result);
            return ResponseEntity.badRequest().body(response);
        }
    }
}