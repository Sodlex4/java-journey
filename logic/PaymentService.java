package logic;

public class PaymentService {

    public String deposit(UserAccount user, double amount) {
        try {
            user.deposit(amount);
            return "Deposited $" + amount + " successfully for " + user.getUsername();
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    public String withdraw(UserAccount user, double amount) {
        try {
            if (!user.withdraw(amount)) return "Insufficient funds for " + user.getUsername();
            return "Withdrew $" + amount + " successfully from " + user.getUsername();
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    public double checkBalance(UserAccount user) {
        return user.getBalance();
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
}
