package com.mpesa.dto;

import jakarta.validation.constraints.*;

public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username must be alphanumeric")
    private String username;

    @DecimalMin(value = "0.0", message = "Initial balance cannot be negative")
    private Double balance;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
}
