package service;

import model.Transaction;
import model.UserAccount;

public class PaymentService {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public PaymentService(UserAccount safaricom) {
        this.accountService = new AccountService(safaricom);
        this.transactionService = new TransactionService();
    }

    public String deposit(UserAccount user, double amount) {
        if (amount <= 0) {
            transactionService.recordTransaction(user, "DEPOSIT", amount, "FAILED", null, user.getUsername());
            return "Invalid amount.";
        }

        boolean success = accountService.deposit(user, amount);
        if (success) {
            Transaction tx = transactionService.createTransaction("DEPOSIT", amount, "SUCCESS", "SYSTEM", user.getUsername());
            transactionService.addTransaction(user, tx);
            return transactionService.formatMessage("Received", amount, user, tx);
        }
        return "Deposit failed.";
    }

    public String withdraw(UserAccount user, double amount) {
        if (amount <= 0) {
            transactionService.recordTransaction(user, "WITHDRAW", amount, "FAILED", user.getUsername(), null);
            return "Invalid amount.";
        }

        double fee = calculateFee(amount);

        boolean success = accountService.withdraw(user, amount, fee);
        if (success) {
            Transaction tx = transactionService.createTransaction("WITHDRAW", amount, "SUCCESS", user.getUsername(), "AGENT");
            transactionService.addTransaction(user, tx);
            String result = transactionService.formatMessage("Withdrawn", amount, user, tx);
            if (fee > 0) {
                result += "\nFee collected: KES " + fee;
            }
            return result;
        }
        transactionService.recordTransaction(user, "WITHDRAW", amount, "FAILED", user.getUsername(), null);
        return "M-PESA FAILED\nInsufficient funds including fees.";
    }

    public String sendMoney(UserAccount from, UserAccount to, double amount) {
        if (amount <= 0) {
            return "Invalid amount.";
        }

        double fee = calculateFee(amount);

        boolean success = accountService.withdraw(from, amount, fee);
        if (!success) {
            transactionService.recordTransaction(from, "TRANSFER", amount, "FAILED", from.getUsername(), to.getUsername());
            return "M-PESA FAILED: Insufficient funds including fees.";
        }

        accountService.deposit(to, amount);

        Transaction txSent = transactionService.createTransaction("TRANSFER_SENT", amount, "SUCCESS", from.getUsername(), to.getUsername());
        Transaction txReceived = transactionService.createTransaction("TRANSFER_RECEIVED", amount, "SUCCESS", from.getUsername(), to.getUsername());
        transactionService.addTransaction(from, txSent);
        transactionService.addTransaction(to, txReceived);

        String result = "M-PESA CONFIRMED\nYou sent KES " + amount + " to " + to.getUsername() +
                "\nNew balance: KES " + from.getBalance();
        if (fee > 0) {
            result += "\nFee collected: KES " + fee;
        }
        return result;
    }

    public String buyAirtime(UserAccount user, double amount) {
        double fee = calculateFee(amount);

        boolean success = accountService.withdraw(user, amount, fee);
        if (!success) {
            transactionService.recordTransaction(user, "AIRTIME", amount, "FAILED", user.getUsername(), "SAFARICOM");
            return "Insufficient funds including fees.";
        }

        Transaction tx = transactionService.createTransaction("AIRTIME", amount, "SUCCESS", user.getUsername(), "SAFARICOM");
        transactionService.addTransaction(user, tx);

        String result = "Airtime purchased\nTXID: " + tx.getId();
        if (fee > 0) {
            result += "\nFee collected: KES " + fee;
        }
        return result;
    }

    public String lipaNaMpesa(UserAccount user, double amount, String business) {
        double fee = calculateFee(amount);

        boolean success = accountService.withdraw(user, amount, fee);
        if (!success) {
            transactionService.recordTransaction(user, "LIPA", amount, "FAILED", user.getUsername(), business);
            return "Insufficient funds including fees.";
        }

        Transaction tx = transactionService.createTransaction("LIPA", amount, "SUCCESS", user.getUsername(), business);
        transactionService.addTransaction(user, tx);

        String result = "Paid " + business + "\nTXID: " + tx.getId();
        if (fee > 0) {
            result += "\nFee collected: KES " + fee;
        }
        return result;
    }

    public String requestLoan(UserAccount user, double amount) {
        if (amount > 1000) {
            transactionService.recordTransaction(user, "LOAN", amount, "FAILED", "SYSTEM", user.getUsername());
            return "Loan denied.";
        }

        accountService.deposit(user, amount);
        Transaction tx = transactionService.createTransaction("LOAN", amount, "SUCCESS", "SYSTEM", user.getUsername());
        transactionService.addTransaction(user, tx);

        return "Loan approved\nTXID: " + tx.getId();
    }

    public double checkBalance(UserAccount user) {
        return accountService.checkBalance(user);
    }

    public void printTransactions(UserAccount user) {
        transactionService.printTransactions(user);
    }

    private double calculateFee(double amount) {
        if (amount <= 100) return 0;
        else if (amount <= 500) return 13;
        else if (amount <= 1000) return 25;
        else return 30;
    }
}