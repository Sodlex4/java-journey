package com.mpesa.dto;

import jakarta.validation.constraints.*;

public class ChangePinRequest {
    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotBlank(message = "Current PIN is required")
    private String currentPin;

    @NotBlank(message = "New PIN is required")
    @Size(min = 4, max = 4, message = "PIN must be 4 digits")
    @Pattern(regexp = "^\\d{4}$", message = "PIN must be numeric")
    private String newPin;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getCurrentPin() { return currentPin; }
    public void setCurrentPin(String currentPin) { this.currentPin = currentPin; }
    public String getNewPin() { return newPin; }
    public void setNewPin(String newPin) { this.newPin = newPin; }
}
