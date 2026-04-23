package service;

import java.sql.SQLException;
import model.Transaction;
import model.UserAccount;
import java.util.List;

public class PaymentService {

    private static final double MAX_AMOUNT = 500000;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final FeeCalculator feeCalculator;
    private final DatabaseService dbService;

    public PaymentService(UserAccount safaricom, DatabaseService dbService) {
        this.accountService = new AccountService(safaricom);
        this.transactionService = new TransactionService();
        this.feeCalculator = new FeeCalculator();
        this.dbService = dbService;
    }

    public boolean verifyPin(int userId, String pin) {
        return dbService.verifyPin(userId, pin);
    }

    public boolean changePin(int userId, String currentPin, String newPin) {
        if (!dbService.verifyPin(userId, currentPin)) {
            return false;
        }
        return dbService.updatePin(userId, newPin);
    }

    private void saveToDb(Transaction tx, int userId) {
        if (dbService != null) {
            dbService.saveTransaction(tx, userId);
        }
    }

    public String deposit(UserAccount user, double amount) {
        if (amount <= 0) {
            transactionService.recordTransaction(user, "DEPOSIT", amount, "FAILED", null, user.getUsername());
            return "Invalid amount.";
        }
        if (amount > MAX_AMOUNT) {
            return "Amount exceeds maximum limit of KES " + MAX_AMOUNT;
        }

        boolean success = accountService.deposit(user, amount);
        if (success) {
            Transaction tx = transactionService.createTransaction("DEPOSIT", amount, "SUCCESS", "SYSTEM", user.getUsername());
            transactionService.addTransaction(user, tx);
            saveToDb(tx, user.getId());
            return transactionService.formatMessage("Received", amount, user, tx);
        }
        return "Deposit failed.";
    }

    public String withdraw(UserAccount user, double amount, String pin) {
        if (dbService.isLocked(user.getId())) {
            return "Account locked. Contact support.";
        }
        int attempts = dbService.getFailedAttempts(user.getId());
        if (!dbService.verifyPin(user.getId(), pin)) {
            transactionService.recordTransaction(user, "WITHDRAW", amount, "FAILED", user.getUsername(), null);
            int remaining = 3 - attempts;
            if (remaining <= 0) {
                return "Account locked. Contact support.";
            }
            return "Incorrect PIN. " + remaining + " attempt(s) remaining.";
        }
        if (amount <= 0) {
            transactionService.recordTransaction(user, "WITHDRAW", amount, "FAILED", user.getUsername(), null);
            return "Invalid amount.";
        }
        if (amount > MAX_AMOUNT) {
            return "Amount exceeds maximum limit of KES " + MAX_AMOUNT;
        }

        double fee = feeCalculator.calculate(amount);

        boolean success = accountService.withdraw(user, amount, fee);
        if (success) {
            Transaction tx = transactionService.createTransaction("WITHDRAW", amount, "SUCCESS", user.getUsername(), "AGENT");
            transactionService.addTransaction(user, tx);
            saveToDb(tx, user.getId());
            String result = transactionService.formatMessage("Withdrawn", amount, user, tx);
            if (fee > 0) {
                result += "\nFee collected: KES " + fee;
            }
            return result;
        }
        transactionService.recordTransaction(user, "WITHDRAW", amount, "FAILED", user.getUsername(), null);
        return "M-PESA FAILED\nInsufficient funds including fees.";
    }

    public String withdraw(UserAccount user, double amount) {
        return withdraw(user, amount, "1234");
    }

    public String sendMoney(UserAccount from, UserAccount to, double amount, String pin) {
        if (dbService.isLocked(from.getId())) {
            return "Account locked. Contact support.";
        }
        int attempts = dbService.getFailedAttempts(from.getId());
        if (!dbService.verifyPin(from.getId(), pin)) {
            transactionService.recordTransaction(from, "TRANSFER", amount, "FAILED", from.getUsername(), to.getUsername());
            int remaining = 3 - attempts;
            if (remaining <= 0) {
                return "Account locked. Contact support.";
            }
            return "Incorrect PIN. " + remaining + " attempt(s) remaining.";
        }
        if (amount <= 0) {
            transactionService.recordTransaction(from, "TRANSFER", amount, "FAILED", from.getUsername(), to.getUsername());
            return "Invalid amount.";
        }
        if (amount > MAX_AMOUNT) {
            return "Amount exceeds maximum limit of KES " + MAX_AMOUNT;
        }

        double fee = feeCalculator.calculate(amount);

        boolean withdrawSuccess = accountService.withdraw(from, amount, fee);
        if (!withdrawSuccess) {
            transactionService.recordTransaction(from, "TRANSFER", amount, "FAILED", from.getUsername(), to.getUsername());
            return "M-PESA FAILED: Insufficient funds including fees.";
        }

        boolean depositSuccess = false;
        try {
            depositSuccess = accountService.deposit(to, amount);
        } catch (Exception e) {
            accountService.refund(from, amount, fee);
            transactionService.recordTransaction(from, "TRANSFER", amount, "FAILED", from.getUsername(), to.getUsername());
            return "M-PESA FAILED: Transfer failed. Money refunded.";
        }

        if (!depositSuccess) {
            // ATOMICITY: Rollback - restore money to sender
            accountService.refund(from, amount, fee);
            transactionService.recordTransaction(from, "TRANSFER", amount, "FAILED", from.getUsername(), to.getUsername());
            return "M-PESA FAILED: Transfer failed. Money refunded.";
        }

        Transaction txSent = transactionService.createTransaction("TRANSFER_SENT", amount, "SUCCESS", from.getUsername(), to.getUsername());
        Transaction txReceived = transactionService.createTransaction("TRANSFER_RECEIVED", amount, "SUCCESS", from.getUsername(), to.getUsername());
        transactionService.addTransaction(from, txSent);
        transactionService.addTransaction(to, txReceived);
        saveToDb(txSent, from.getId());
        saveToDb(txReceived, to.getId());

        String result = "M-PESA CONFIRMED\nYou sent KES " + amount + " to " + to.getUsername() +
                "\nNew balance: KES " + from.getBalance();
        if (fee > 0) {
            result += "\nFee collected: KES " + fee;
        }
        return result;
    }

    public String sendMoney(UserAccount from, UserAccount to, double amount) {
        return sendMoney(from, to, amount, "1234");
    }

    public String buyAirtime(UserAccount user, double amount) {
        if (amount <= 0) {
            return "Invalid amount.";
        }
        if (amount > MAX_AMOUNT) {
            return "Amount exceeds maximum limit of KES " + MAX_AMOUNT;
        }

        double fee = feeCalculator.calculate(amount);

        boolean success = accountService.withdraw(user, amount, fee);
        if (!success) {
            transactionService.recordTransaction(user, "AIRTIME", amount, "FAILED", user.getUsername(), "SAFARICOM");
            return "Insufficient funds including fees.";
        }

        Transaction tx = transactionService.createTransaction("AIRTIME", amount, "SUCCESS", user.getUsername(), "SAFARICOM");
        transactionService.addTransaction(user, tx);
        saveToDb(tx, user.getId());

        String result = "Airtime purchased\nTXID: " + tx.getId();
        if (fee > 0) {
            result += "\nFee collected: KES " + fee;
        }
        return result;
    }

    public String lipaNaMpesa(UserAccount user, double amount, String business) {
        if (amount <= 0) {
            return "Invalid amount.";
        }
        if (amount > MAX_AMOUNT) {
            return "Amount exceeds maximum limit of KES " + MAX_AMOUNT;
        }

        double fee = feeCalculator.calculate(amount);

        boolean success = accountService.withdraw(user, amount, fee);
        if (!success) {
            transactionService.recordTransaction(user, "LIPA", amount, "FAILED", user.getUsername(), business);
            return "Insufficient funds including fees.";
        }

        Transaction tx = transactionService.createTransaction("LIPA", amount, "SUCCESS", user.getUsername(), business);
        transactionService.addTransaction(user, tx);
        saveToDb(tx, user.getId());

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
        saveToDb(tx, user.getId());

        return "Loan approved\nTXID: " + tx.getId();
    }

    public double checkBalance(UserAccount user) {
        return accountService.checkBalance(user);
    }

    public void printTransactions(UserAccount user) {
        transactionService.printTransactions(user);
    }

    public String depositToSavings(UserAccount user, double amount) {
        if (amount <= 0) {
            return "Invalid amount.";
        }
        if (amount > MAX_AMOUNT) {
            return "Amount exceeds maximum limit of KES " + MAX_AMOUNT;
        }

        double fee = feeCalculator.calculate(amount);
        
        if (!accountService.withdraw(user, amount, 0)) {
            return "Insufficient funds.";
        }
        
        boolean success = dbService.createSavingsAccount(user.getId());
        if (!success && dbService.getSavingsBalance(user.getId()) == 0) {
            dbService.createSavingsAccount(user.getId());
        }
        
        success = dbService.depositToSavings(user.getId(), amount);
        
        if (!success) {
            accountService.deposit(user, amount);
            return "Savings deposit failed.";
        }
        
        Transaction tx = transactionService.createTransaction("SAVINGS_DEPOSIT", amount, "SUCCESS", user.getUsername(), "SIMI");
        transactionService.addTransaction(user, tx);
        saveToDb(tx, user.getId());

        return "Saved to Simi\nAmount: KES " + amount + "\nInterest: " + dbService.getSavingsInterestRate(user.getId()) + "% p.a.";
    }

    public String withdrawFromSavings(UserAccount user, double amount) {
        if (amount <= 0) {
            return "Invalid amount.";
        }

        double savingsBalance = dbService.getSavingsBalance(user.getId());
        if (amount > savingsBalance) {
            return "Insufficient savings balance.";
        }

        boolean success = dbService.withdrawFromSavings(user.getId(), amount);
        
        if (!success) {
            return "Savings withdrawal failed.";
        }
        
        accountService.deposit(user, amount);
        
        Transaction tx = transactionService.createTransaction("SAVINGS_WITHDRAW", amount, "SUCCESS", "SIMI", user.getUsername());
        transactionService.addTransaction(user, tx);
        saveToDb(tx, user.getId());

        return "Withdrawn from Simi\nAmount: KES " + amount + "\nNew balance: KES " + dbService.getSavingsBalance(user.getId());
    }

    public String checkSavingsBalance(UserAccount user) {
        double balance = dbService.getSavingsBalance(user.getId());
        double rate = dbService.getSavingsInterestRate(user.getId());
        double annualInterest = balance * (rate / 100);
        
        return "Simi Savings\nBalance: KES " + balance + "\nInterest Rate: " + rate + "% p.a.\nProjected Annual Interest: KES " + annualInterest;
    }

    public String createBillSplit(UserAccount creator, int participantCount, double totalAmount, String title) {
        if (totalAmount <= 0) {
            return "Invalid amount.";
        }
        if (totalAmount > MAX_AMOUNT) {
            return "Amount exceeds maximum limit of KES " + MAX_AMOUNT;
        }

        double eachAmount = totalAmount / participantCount;
        
        for (int i = 0; i < participantCount; i++) {
            if (!accountService.withdraw(creator, eachAmount, 0)) {
                return "Insufficient funds for split.\nEach share: KES " + eachAmount;
            }
        }

        int splitId = dbService.createBillSplit(creator.getId(), totalAmount, participantCount, title);
        
        Transaction tx = transactionService.createTransaction("BILL_SPLIT", totalAmount, "SUCCESS", creator.getUsername(), "SPLIT");
        transactionService.addTransaction(creator, tx);
        saveToDb(tx, creator.getId());

        return "Bill Split Created\nTitle: " + title + "\nTotal: KES " + totalAmount + 
               "\nSplit " + participantCount + " ways: KES " + eachAmount + " each\nSplit ID: " + splitId;
    }

    public String payBillSplit(UserAccount user, int splitId) {
        if (!dbService.paySplitShare(splitId, user.getId())) {
            return "Payment failed.";
        }
        
        Transaction tx = transactionService.createTransaction("BILL_SPLIT_PAID", 0, "SUCCESS", user.getUsername(), "SPLIT");
        transactionService.addTransaction(user, tx);
        saveToDb(tx, user.getId());

        return "BillSplit Paid\nSplit ID: " + splitId;
    }

    public String viewBillSplits(UserAccount user) {
        List<String> splits = dbService.getUserBillSplits(user.getId());
        
        if (splits.isEmpty()) {
            return "No bill splits found.";
        }
        
        StringBuilder sb = new StringBuilder("Your Bill Splits:\n");
        for (String split : splits) {
            sb.append(split).append("\n");
        }
        return sb.toString();
    }

    }