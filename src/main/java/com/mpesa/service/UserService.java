package com.mpesa.service;

import com.mpesa.exception.PaymentException;
import com.mpesa.model.User;
import com.mpesa.model.Transaction;
import com.mpesa.repository.UserRepository;
import com.mpesa.repository.TransactionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private static final double MAX_AMOUNT = 500000;
    private static final int MAX_ATTEMPTS = 3;
    private static final int LOCK_DURATION = 300;
    
    public UserService(UserRepository userRepository, TransactionRepository transactionRepository, 
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
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
    public User createUser(String username, String pin, Double initialBalance) {
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        User user = new User(username, initialBalance != null ? initialBalance : 0.0);
        user.setPin(passwordEncoder.encode(pin));
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
        
        if (passwordEncoder.matches(pin, user.getPin())) {
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
        user.setPin(passwordEncoder.encode(newPin));
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
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new PaymentException("User not found."));
        
        user.deposit(amount);
        userRepository.save(user);
        
        return "Deposit successful. New balance: KES " + user.getBalance();
    }
    
    @Transactional
    public String withdraw(Integer userId, Double amount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new PaymentException("User not found."));
        
        double fee = calculateFee(amount);
        double total = amount + fee;
        
        if (!user.withdraw(total)) {
            throw new PaymentException("Insufficient funds.");
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
        User from = userRepository.findById(fromUserId)
            .orElseThrow(() -> new PaymentException("Sender not found."));
        User to = userRepository.findById(toUserId)
            .orElseThrow(() -> new PaymentException("Recipient not found."));
        
        if (fromUserId.equals(toUserId)) {
            throw new PaymentException("Cannot transfer to yourself.");
        }
        
        double fee = calculateFee(amount);
        double total = amount + fee;
        
        if (!from.withdraw(total)) {
            throw new PaymentException("Insufficient funds.");
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
    
    public Double getBalance(Integer userId) {
        return userRepository.findById(userId).map(User::getBalance).orElse(null);
    }
    
    public List<Transaction> getTransactionHistory(Integer userId) {
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId);
    }
}