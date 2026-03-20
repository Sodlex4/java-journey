package logic;

import java.util.ArrayList;
import java.util.List;

public class UserAccount {

    private final String username;   // account name
    private double balance;          // current balance
    private List<Transaction> transactions;  // transaction history

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

    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit must be positive.");
        balance += amount;
        transactions.add(new Transaction("Deposit", amount));
    }

    public boolean withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdraw must be positive.");
        if (amount > balance) return false;
        balance -= amount;
        transactions.add(new Transaction("Withdraw", amount));
        return true;
    }
}
