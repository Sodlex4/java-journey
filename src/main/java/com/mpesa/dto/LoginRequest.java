package com.mpesa.dto;

import jakarta.validation.constraints.*;

public class LoginRequest {
    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotBlank(message = "PIN is required")
    @Size(min = 4, max = 4, message = "PIN must be 4 digits")
    private String pin;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
}
