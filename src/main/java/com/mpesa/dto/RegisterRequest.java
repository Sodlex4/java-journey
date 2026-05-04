package com.mpesa.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username must be alphanumeric")
    private String username;

    @NotBlank(message = "PIN is required")
    @Size(min = 4, max = 4, message = "PIN must be 4 digits")
    @Pattern(regexp = "^\\d{4}$", message = "PIN must be numeric")
    private String pin;

    @NotNull(message = "Initial balance cannot be negative")
    @DecimalMin(value = "0.0", message = "Initial balance cannot be negative")
    private BigDecimal balance;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
