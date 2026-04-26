package com.mpesa.service;

import com.mpesa.model.User;
import com.mpesa.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private static final double MAX_AMOUNT = 500000;
    private static final int MAX_ATTEMPTS = 3;
    private static final int LOCK_DURATION = 300;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Transactional
    public User createUser(String username, Double initialBalance) {
        if (username == null || username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("Username must be 3-50 characters");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Username must be alphanumeric");
        }
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        User user = new User(username, initialBalance != null ? initialBalance : 0.0);
        user.setPin(BCrypt.hashpw("1234", BCrypt.gensalt()));
        return userRepository.save(user);
    }
    
    public boolean verifyPin(Integer userId, String pin) {
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) return false;
        
        User user = optUser.get();
        if (user.getLocked()) {
            if (user.getLockUntil() != null && System.currentTimeMillis() > user.getLockUntil()) {
                user.setLocked(false);
                user.setFailedAttempts(0);
                user.setLockUntil(null);
                userRepository.save(user);
            } else {
                return false;
            }
        }
        
        if (BCrypt.checkpw(pin, user.getPin())) {
            user.setFailedAttempts(0);
            userRepository.save(user);
            return true;
        }
        
        user.setFailedAttempts(user.getFailedAttempts() + 1);
        if (user.getFailedAttempts() >= MAX_ATTEMPTS) {
            user.setLocked(true);
            user.setLockUntil(System.currentTimeMillis() + (LOCK_DURATION * 1000));
            user.setLockCount(user.getLockCount() + 1);
        }
        userRepository.save(user);
        return false;
    }
    
    @Transactional
    public boolean changePin(Integer userId, String currentPin, String newPin) {
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) return false;
        if (!verifyPin(userId, currentPin)) return false;
        
        User user = optUser.get();
        user.setPin(BCrypt.hashpw(newPin, BCrypt.gensalt()));
        userRepository.save(user);
        return true;
    }
    
    public boolean isLocked(Integer userId) {
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) return false;
        
        User user = optUser.get();
        if (!user.getLocked()) return false;
        
        if (user.getLockUntil() != null && System.currentTimeMillis() > user.getLockUntil()) {
            user.setLocked(false);
            user.setFailedAttempts(0);
            user.setLockUntil(null);
            userRepository.save(user);
            return false;
        }
        return true;
    }
    
    public Long getLockRemainingSeconds(Integer userId) {
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) return 0L;
        
        User user = optUser.get();
        if (user.getLockUntil() == null) return 0L;
        
        long remaining = (user.getLockUntil() - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
    
    @Transactional
    public String deposit(Integer userId, Double amount) {
        if (amount == null || amount <= 0) {
            return "Invalid amount.";
        }
        if (amount > MAX_AMOUNT) {
            return "Amount exceeds maximum limit of KES " + MAX_AMOUNT;
        }
        
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) return "User not found.";
        
        User user = optUser.get();
        user.deposit(amount);
        userRepository.save(user);
        
        return "Deposit successful. New balance: KES " + user.getBalance();
    }
    
    @Transactional
    public String withdraw(Integer userId, Double amount) {
        if (amount == null || amount <= 0) {
            return "Invalid amount.";
        }
        if (amount > MAX_AMOUNT) {
            return "Amount exceeds maximum limit of KES " + MAX_AMOUNT;
        }
        
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) return "User not found.";
        
        User user = optUser.get();
        double fee = calculateFee(amount);
        double total = amount + fee;
        
        if (!user.withdraw(total)) {
            return "Insufficient funds.";
        }
        
        userRepository.save(user);
        String result = "Withdrawal successful. Amount: KES " + amount;
        if (fee > 0) {
            result += ", Fee: KES " + fee;
        }
        result += ". New balance: KES " + user.getBalance();
        return result;
    }
    
    @Transactional
    public String transfer(Integer fromUserId, Integer toUserId, Double amount) {
        if (amount == null || amount <= 0) {
            return "Invalid amount.";
        }
        if (amount > MAX_AMOUNT) {
            return "Amount exceeds maximum limit of KES " + MAX_AMOUNT;
        }
        
        Optional<User> optFrom = userRepository.findById(fromUserId);
        Optional<User> optTo = userRepository.findById(toUserId);
        
        if (optFrom.isEmpty()) return "Sender not found.";
        if (optTo.isEmpty()) return "Recipient not found.";
        if (fromUserId.equals(toUserId)) return "Cannot transfer to yourself.";
        
        User from = optFrom.get();
        User to = optTo.get();
        double fee = calculateFee(amount);
        double total = amount + fee;
        
        if (!from.withdraw(total)) {
            return "Insufficient funds.";
        }
        
        to.deposit(amount);
        
        userRepository.save(from);
        userRepository.save(to);
        
        return "Transfer successful. Sent KES " + amount + " to " + to.getUsername() + 
               ". New balance: KES " + from.getBalance();
    }
    
    private double calculateFee(double amount) {
        if (amount <= 100) return 0;
        if (amount <= 500) return 13;
        if (amount <= 1000) return 25;
        return 30;
    }
}