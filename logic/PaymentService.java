package logic;

public class PaymentService {

    public String deposit(UserAccount user, double amount) {
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("Invalid amount. Enter a value greater than 0.");
            }

            user.deposit(amount);

            return "M-PESA CONFIRMED\n" +
                   "You have received KES " + amount + "\n" +
                   "New balance: KES " + user.getBalance() + "\n" +
                   "Account: " + user.getUsername();

        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    public String withdraw(UserAccount user, double amount) {
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("Invalid amount. Enter a value greater than 0.");
            }

            if (!user.withdraw(amount)) {
                return "M-PESA FAILED\nInsufficient funds for " + user.getUsername();
            }

            return "M-PESA CONFIRMED\n" +
                   "KES " + amount + " sent successfully\n" +
                   "New balance: KES " + user.getBalance() + "\n" +
                   "Account: " + user.getUsername();

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
