package logic;

import java.util.ArrayList;
import java.util.List;

public class UserAccount {

    private final String username;
    private double balance;
    private final List<Transaction> transactions;

    public UserAccount(String username, double initialBalance) {
        this.username = username;
        this.balance = initialBalance;
        this.transactions = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    public double getBalance() {
        return balance;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    // ✅ Controlled deposit
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit must be positive.");
        }
        balance += amount;
    }

    // ✅ Controlled withdraw
    public boolean withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdraw must be positive.");
        }

        if (amount > balance) {
            return false;
        }

        balance -= amount;
        return true;
    }

    // ✅ Add transaction safely
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }
}
