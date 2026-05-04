package com.mpesa.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class WithdrawRequest {
    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @DecimalMax(value = "500000", message = "Amount exceeds maximum limit")
    private BigDecimal amount;

    @NotBlank(message = "PIN is required")
    private String pin;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
}
