package controller;

import java.util.Scanner;
import logic.PaymentService;
import logic.UserAccount;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        PaymentService service = new PaymentService();

        // Create a user account
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();

        System.out.print("Enter initial balance: ");
        double initialBalance = scanner.nextDouble();
        scanner.nextLine(); // consume the newline

        UserAccount account = new UserAccount(username, initialBalance);

        while (true) {
            showMenu();

            System.out.print("Choose option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    // Check balance
                    double balance = service.checkBalance(account);
                    System.out.println("Balance for " + account.getUsername() + ": $" + balance);
                    break;

                case 2:
                    // Deposit
                    System.out.print("Enter amount to deposit: ");
                    double depositAmount = scanner.nextDouble();
                    scanner.nextLine();
                    String depositResult = service.deposit(account, depositAmount);
                    System.out.println(depositResult);
                    break;

                case 3:
                    // Withdraw
                    System.out.print("Enter amount to withdraw: ");
                    double withdrawAmount = scanner.nextDouble();
                    scanner.nextLine();
                    String withdrawResult = service.withdraw(account, withdrawAmount);
                    System.out.println(withdrawResult);
                    break;

                case 4:
                    // Transaction history
                    service.printTransactions(account);
                    break;

                case 5:
                    // Exit
                    System.out.println("Goodbye 👋");
                    scanner.close();
                    return;

                default:
                    System.out.println("Invalid option!");
            }

            System.out.println();
        }
    }

    static void showMenu() {
        System.out.println("=== Payment System ===");
        System.out.println("1. Check Balance");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. View Transactions");
        System.out.println("5. Exit");
    }
}
