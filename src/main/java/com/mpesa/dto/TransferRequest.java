package com.mpesa.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class TransferRequest {
    @NotNull(message = "Sender ID is required")
    private Integer fromUserId;

    @NotNull(message = "Recipient ID is required")
    @Min(value = 1, message = "Recipient ID must be positive")
    private Integer toUserId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @DecimalMax(value = "500000", message = "Amount exceeds maximum limit")
    private BigDecimal amount;

    @NotBlank(message = "PIN is required")
    private String pin;

    public Integer getFromUserId() { return fromUserId; }
    public void setFromUserId(Integer fromUserId) { this.fromUserId = fromUserId; }
    public Integer getToUserId() { return toUserId; }
    public void setToUserId(Integer toUserId) { this.toUserId = toUserId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
}
