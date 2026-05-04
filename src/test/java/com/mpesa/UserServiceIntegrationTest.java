package com.mpesa;

import com.mpesa.exception.PaymentException;
import com.mpesa.model.User;
import com.mpesa.repository.UserRepository;
import com.mpesa.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class UserServiceIntegrationTest {

    private static final String TEST_PIN = "1234";

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testCreateUser() {
        String username = "testuser_" + System.currentTimeMillis();
        User user = userService.createUser(username, TEST_PIN, new BigDecimal("1000.0"));
        
        assertNotNull(user.getId());
        assertEquals(username, user.getUsername());
        assertEquals(0, new BigDecimal("1000.0").compareTo(user.getBalance()));
        
        userRepository.delete(user);
    }

    @Test
    public void testVerifyPin() {
        String username = "pinuser_" + System.currentTimeMillis();
        User user = userService.createUser(username, TEST_PIN, new BigDecimal("500.0"));
        
        assertTrue(userService.verifyPin(user.getId(), TEST_PIN));
        assertFalse(userService.verifyPin(user.getId(), "wrong"));
        
        userRepository.delete(user);
    }

    @Test
    public void testDeposit() {
        String username = "deposituser_" + System.currentTimeMillis();
        User user = userService.createUser(username, TEST_PIN, new BigDecimal("100.0"));
        
        String result = userService.deposit(user.getId(), new BigDecimal("200.0"));
        assertTrue(result.contains("successful"));
        
        User updated = userService.findById(user.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("300.0").compareTo(updated.getBalance()));
        
        userRepository.delete(user);
    }

    @Test
    public void testWithdraw() {
        String username = "withdrawuser_" + System.currentTimeMillis();
        User user = userService.createUser(username, TEST_PIN, new BigDecimal("500.0"));
        
        String result = userService.withdraw(user.getId(), new BigDecimal("100.0"));
        assertTrue(result.contains("successful"));
        
        User updated = userService.findById(user.getId()).orElseThrow();
        assertTrue(updated.getBalance().compareTo(new BigDecimal("500.0")) < 0);
        
        userRepository.delete(user);
    }

    @Test
    public void testTransfer() {
        String senderName = "sender_" + System.currentTimeMillis();
        String receiverName = "receiver_" + System.currentTimeMillis();
        
        User sender = userService.createUser(senderName, TEST_PIN, new BigDecimal("1000.0"));
        User receiver = userService.createUser(receiverName, TEST_PIN, new BigDecimal("100.0"));
        
        String result = userService.transfer(sender.getId(), receiver.getId(), new BigDecimal("200.0"));
        assertTrue(result.contains("successful"));
        
        User updatedSender = userService.findById(sender.getId()).orElseThrow();
        User updatedReceiver = userService.findById(receiver.getId()).orElseThrow();
        
        assertEquals(0, new BigDecimal("800.0").compareTo(updatedSender.getBalance().add(new BigDecimal("13"))));
        assertEquals(0, new BigDecimal("300.0").compareTo(updatedReceiver.getBalance()));
        
        userRepository.delete(sender);
        userRepository.delete(receiver);
    }

    @Test
    public void testInsufficientFunds() {
        String username = "fundsuser_" + System.currentTimeMillis();
        User user = userService.createUser(username, TEST_PIN, new BigDecimal("50.0"));
        
        assertThrows(PaymentException.class, () -> userService.withdraw(user.getId(), new BigDecimal("100.0")));
        
        userRepository.delete(user);
    }
}
