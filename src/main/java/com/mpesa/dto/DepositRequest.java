package com.mpesa.dto;

import jakarta.validation.constraints.*;

public class DepositRequest {
    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @DecimalMax(value = "500000", message = "Amount exceeds maximum limit")
    private Double amount;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
}
