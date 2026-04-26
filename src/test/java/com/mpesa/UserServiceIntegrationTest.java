package com.mpesa;

import com.mpesa.model.User;
import com.mpesa.repository.UserRepository;
import com.mpesa.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testCreateUser() {
        String username = "testuser_" + System.currentTimeMillis();
        User user = userService.createUser(username, 1000.0);
        
        assertNotNull(user.getId());
        assertEquals(username, user.getUsername());
        assertEquals(1000.0, user.getBalance());
        
        userRepository.delete(user);
    }

    @Test
    public void testVerifyPin() {
        String username = "pinuser_" + System.currentTimeMillis();
        User user = userService.createUser(username, 500.0);
        
        assertTrue(userService.verifyPin(user.getId(), "1234"));
        assertFalse(userService.verifyPin(user.getId(), "wrong"));
        
        userRepository.delete(user);
    }

    @Test
    public void testDeposit() {
        String username = "deposituser_" + System.currentTimeMillis();
        User user = userService.createUser(username, 100.0);
        
        String result = userService.deposit(user.getId(), 200.0);
        assertTrue(result.contains("successful"));
        
        User updated = userService.findById(user.getId()).orElseThrow();
        assertEquals(300.0, updated.getBalance());
        
        userRepository.delete(user);
    }

    @Test
    public void testWithdraw() {
        String username = "withdrawuser_" + System.currentTimeMillis();
        User user = userService.createUser(username, 500.0);
        
        String result = userService.withdraw(user.getId(), 100.0);
        assertTrue(result.contains("successful"));
        
        User updated = userService.findById(user.getId()).orElseThrow();
        assertTrue(updated.getBalance() < 500.0);
        
        userRepository.delete(user);
    }

    @Test
    public void testTransfer() {
        String senderName = "sender_" + System.currentTimeMillis();
        String receiverName = "receiver_" + System.currentTimeMillis();
        
        User sender = userService.createUser(senderName, 1000.0);
        User receiver = userService.createUser(receiverName, 100.0);
        
        String result = userService.transfer(sender.getId(), receiver.getId(), 200.0);
        assertTrue(result.contains("successful"));
        
        User updatedSender = userService.findById(sender.getId()).orElseThrow();
        User updatedReceiver = userService.findById(receiver.getId()).orElseThrow();
        
        assertEquals(800.0, updatedSender.getBalance() + 13, 1);
        assertEquals(300.0, updatedReceiver.getBalance());
        
        userRepository.delete(sender);
        userRepository.delete(receiver);
    }

    @Test
    public void testInsufficientFunds() {
        String username = "fundsuser_" + System.currentTimeMillis();
        User user = userService.createUser(username, 50.0);
        
        String result = userService.withdraw(user.getId(), 100.0);
        assertTrue(result.contains("Insufficient"));
        
        userRepository.delete(user);
    }
}