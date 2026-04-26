package com.mpesa.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private Double balance = 0.0;
    
    @Column(length = 60)
    private String pin;
    
    @Column(name = "failed_attempts")
    private Integer failedAttempts = 0;
    
    private Boolean locked = false;
    
    @Column(name = "lock_until")
    private Long lockUntil;
    
    @Column(name = "lock_count")
    private Integer lockCount = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public User() {}
    
    public User(String username, Double balance) {
        this.username = username;
        this.balance = balance;
    }
    
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public Integer getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; }
    public Boolean getLocked() { return locked; }
    public void setLocked(Boolean locked) { this.locked = locked; }
    public Long getLockUntil() { return lockUntil; }
    public void setLockUntil(Long lockUntil) { this.lockUntil = lockUntil; }
    public Integer getLockCount() { return lockCount; }
    public void setLockCount(Integer lockCount) { this.lockCount = lockCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public void deposit(Double amount) {
        this.balance += amount;
    }
    
    public boolean withdraw(Double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }
}