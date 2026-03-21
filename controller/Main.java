package controller;

import java.util.Scanner;
import logic.PaymentService;
import logic.UserAccount;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        PaymentService service = new PaymentService();

        // Username input
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();

        while (username.trim().isEmpty()) {
            System.out.print("Username cannot be empty. Enter again: ");
            username = scanner.nextLine();
        }

        // Initial balance (safe input)
        double initialBalance = getValidDouble(scanner, "Enter initial balance: ");

        while (initialBalance < 0) {
            System.out.println("Balance cannot be negative.");
            initialBalance = getValidDouble(scanner, "Enter initial balance: ");
        }

        UserAccount account = new UserAccount(username, initialBalance);

        // Main loop
        while (true) {
            showMenu();

            int choice = getValidInt(scanner, "Choose option: ");

            switch (choice) {
                case 1:
                    double balance = service.checkBalance(account);
                    System.out.println("M-PESA BALANCE");
                    System.out.println("KES " + balance);
                    break;

                case 2:
                    double depositAmount = getValidDouble(scanner, "Enter amount to deposit: ");
                    System.out.println("Processing...");
                    System.out.println(service.deposit(account, depositAmount));
                    break;

                case 3:
                    double withdrawAmount = getValidDouble(scanner, "Enter amount to withdraw: ");
                    System.out.println("Processing...");
                    System.out.println(service.withdraw(account, withdrawAmount));
                    break;

                case 4:
                    service.printTransactions(account);
                    break;

                case 5:
                    System.out.println("Goodbye 👋");
                    scanner.close();
                    return;

                default:
                    System.out.println("Invalid option!");
            }

            System.out.println();
        }
    }

    // ✅ Reusable safe double input
    public static double getValidDouble(Scanner scanner, String message) {
        double value;

        while (true) {
            System.out.print(message);

            if (scanner.hasNextDouble()) {
                value = scanner.nextDouble();
                scanner.nextLine(); // consume newline
                return value;
            } else {
                System.out.println("Invalid input. Enter a valid number.");
                scanner.next(); // clear bad input
            }
        }
    }

    // ✅ Reusable safe int input (menu)
    public static int getValidInt(Scanner scanner, String message) {
        int value;

        while (true) {
            System.out.print(message);

            if (scanner.hasNextInt()) {
                value = scanner.nextInt();
                scanner.nextLine(); // consume newline
                return value;
            } else {
                System.out.println("Invalid option. Enter a number.");
                scanner.next();
            }
        }
    }

    static void showMenu() {
        System.out.println("=== M-PESA MENU ===");
        System.out.println("1. Check Balance");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. View Transactions");
        System.out.println("5. Exit");
    }
}
