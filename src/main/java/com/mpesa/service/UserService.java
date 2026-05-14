package com.mpesa.service;

import com.mpesa.config.FeeProperties;
import com.mpesa.exception.PaymentException;
import com.mpesa.model.User;
import com.mpesa.model.Transaction;
import com.mpesa.model.SystemAccount;
import com.mpesa.repository.UserRepository;
import com.mpesa.repository.TransactionRepository;
import com.mpesa.repository.SystemAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final SystemAccountRepository systemAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final FeeProperties feeProperties;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("500000");
    private static final int MAX_ATTEMPTS = 3;
    private static final int LOCK_DURATION = 300;
    
    public UserService(UserRepository userRepository, TransactionRepository transactionRepository,
                       SystemAccountRepository systemAccountRepository,
                       PasswordEncoder passwordEncoder, FeeProperties feeProperties) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.systemAccountRepository = systemAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.feeProperties = feeProperties;
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
    public User createUser(String username, String pin, BigDecimal initialBalance) {
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        User user = new User(username, initialBalance != null ? initialBalance : BigDecimal.ZERO);
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
    public String deposit(Integer userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new PaymentException("User not found"));
        
        user.deposit(amount);
        
        logTransaction("DEPOSIT", amount, BigDecimal.ZERO, "SUCCESS", null, user.getUsername(), user);
        
        return "Deposit successful. New balance: KES " + user.getBalance();
    }
    
    @Transactional
    public String withdraw(Integer userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new PaymentException("User not found"));
        
        BigDecimal fee = calculateFee(amount);
        BigDecimal total = amount.add(fee);
        
        if (!user.withdraw(total)) {
            throw new PaymentException("Insufficient funds");
        }
        
        logTransaction("WITHDRAW", amount, fee, "SUCCESS", user.getUsername(), null, user);
        collectFee(fee);
        
        return "Withdrawal successful. Amount: KES " + amount +
               (fee.compareTo(BigDecimal.ZERO) > 0 ? ", Fee: KES " + fee : "") +
               ". New balance: KES " + user.getBalance();
    }
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public String transfer(Integer fromUserId, Integer toUserId, BigDecimal amount) {
        User from = userRepository.findById(fromUserId)
            .orElseThrow(() -> new PaymentException("Sender not found"));
        User to = userRepository.findById(toUserId)
            .orElseThrow(() -> new PaymentException("Recipient not found"));
        
        if (fromUserId.equals(toUserId)) {
            throw new PaymentException("Cannot transfer to yourself");
        }
        
        BigDecimal fee = calculateFee(amount);
        BigDecimal total = amount.add(fee);
        
        if (!from.withdraw(total)) {
            throw new PaymentException("Insufficient funds");
        }
        
        to.deposit(amount);
        
        logTransaction("TRANSFER", amount, fee, "SUCCESS", from.getUsername(), to.getUsername(), from);
        logTransaction("TRANSFER", amount, BigDecimal.ZERO, "SUCCESS", from.getUsername(), to.getUsername(), to);
        collectFee(fee);
        
        return "Transfer successful. Sent KES " + amount + " to " + to.getUsername() + 
               ". New balance: KES " + from.getBalance();
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Integer userId) {
        return userRepository.findById(userId).map(User::getBalance).orElse(null);
    }
    
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionHistory(Integer userId, Pageable pageable) {
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }
    
    private void collectFee(BigDecimal fee) {
        if (fee.compareTo(BigDecimal.ZERO) <= 0) return;
        SystemAccount safaricom = systemAccountRepository.findByAccountName("SAFARICOM")
            .orElseGet(() -> systemAccountRepository.save(new SystemAccount("SAFARICOM", BigDecimal.ZERO)));
        safaricom.deposit(fee);
    }

    private BigDecimal calculateFee(BigDecimal amount) {
        if (amount.compareTo(feeProperties.getTier1()) <= 0) return feeProperties.getRate1();
        if (amount.compareTo(feeProperties.getTier2()) <= 0) return feeProperties.getRate2();
        if (amount.compareTo(feeProperties.getTier3()) <= 0) return feeProperties.getRate3();
        return feeProperties.getRate4();
    }
    
    private String logTransaction(String type, BigDecimal amount, BigDecimal fee,
            String status, String fromUser, String toUser, User owner) {
        String txId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Transaction tx = new Transaction(txId, type, amount, status, fromUser, toUser);
        tx.setFee(fee);
        tx.setUser(owner);
        transactionRepository.save(tx);
        
        return txId;
    }
}
