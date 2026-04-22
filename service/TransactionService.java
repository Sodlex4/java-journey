package service;

import model.Transaction;
import model.UserAccount;

public class TransactionService {

    public void recordTransaction(UserAccount user, String type, double amount, 
                                   String status, String fromUser, String toUser) {
        Transaction tx = new Transaction(type, amount, status, fromUser, toUser);
        user.addTransaction(tx);
    }

    public Transaction createTransaction(String type, double amount, 
                                           String status, String fromUser, String toUser) {
        return new Transaction(type, amount, status, fromUser, toUser);
    }

    public void addTransaction(UserAccount user, Transaction tx) {
        user.addTransaction(tx);
    }

    public String getTransactions(UserAccount user) {
        StringBuilder sb = new StringBuilder();
        if (user.getTransactions().isEmpty()) {
            return "No transactions yet.";
        }
        for (Transaction t : user.getTransactions()) {
            sb.append(t).append("\n");
        }
        return sb.toString();
    }

    public void printTransactions(UserAccount user) {
        System.out.println("=== Transactions for " + user.getUsername() + " ===");
        if (user.getTransactions().isEmpty()) {
            System.out.println("No transactions yet.");
        } else {
            for (Transaction t : user.getTransactions()) {
                System.out.println(t);
            }
        }
    }

    public String formatMessage(String action, double amount, UserAccount user, Transaction tx) {
        return "M-PESA CONFIRMED\n" +
                action + " KES " + amount + "\n" +
                "Balance: KES " + user.getBalance() + "\n" +
                "TXID: " + tx.getId();
    }
}