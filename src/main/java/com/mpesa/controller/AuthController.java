package com.mpesa.controller;

import com.mpesa.service.UserService;
import com.mpesa.model.User;
import com.mpesa.model.Transaction;
import com.mpesa.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;
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
    public ResponseEntity<?> register(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            Double balance = request.get("balance") != null ? 
                Double.parseDouble(request.get("balance").toString()) : 0.0;
            
            User user = userService.createUser(username, balance);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("userId", user.getId());
            response.put("defaultPin", "1234");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> request) {
        Integer userId = Integer.parseInt(request.get("userId").toString());
        String pin = (String) request.get("pin");
        
        if (userService.isLocked(userId)) {
            long remaining = userService.getLockRemainingSeconds(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Account locked. Try again in " + remaining + " seconds.");
            return ResponseEntity.status(423).body(response);
        }
        
        if (userService.verifyPin(userId, pin)) {
            String token = jwtService.generateToken(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("userId", userId);
            return ResponseEntity.ok(response);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Invalid PIN");
        return ResponseEntity.status(401).body(response);
    }
    
    @GetMapping("/user/{id}")
    public ResponseEntity<?> getUser(@PathVariable Integer id) {
        return userService.findById(id)
            .map(user -> {
                Map<String, Object> response = new HashMap<>();
                response.put("id", user.getId());
                response.put("username", user.getUsername());
                response.put("balance", user.getBalance());
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/balance/{userId}")
    public ResponseEntity<?> getBalance(@PathVariable Integer userId) {
        Double balance = userService.getBalance(userId);
        if (balance == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("balance", balance);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/transactions/{userId}")
    public ResponseEntity<?> getTransactions(@PathVariable Integer userId) {
        List<Transaction> transactions = userService.getTransactionHistory(userId);
        return ResponseEntity.ok(transactions);
    }
}