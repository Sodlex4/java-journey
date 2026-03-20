package logic;

public class PaymentService {

    private double balance = 1000;

    public double checkBalance() {
        return balance;
    }

    public String deposit(double amount) {
        if (amount <= 0) {
            return "Invalid amount!";
        }

        balance += amount;
        return "Deposited successfully.";
    }

    public String withdraw(double amount) {
        if (amount <= 0) {
            return "Invalid amount!";
        }

        if (amount > balance) {
            return "Insufficient funds!";
        }

        balance -= amount;
        return "Withdraw successful.";
    }
}
