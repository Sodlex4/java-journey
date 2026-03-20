package controller;

import java.util.Scanner;
import logic.PaymentService;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        PaymentService service = new PaymentService();

        while (true) {
            showMenu();

            System.out.print("Choose option: ");
            int choice = scanner.nextInt();

            if (choice == 1) {
                double balance = service.checkBalance();
                System.out.println("Balance: $" + balance);

            } else if (choice == 2) {
                System.out.print("Enter amount: ");
                double amount = scanner.nextDouble();

                String result = service.deposit(amount);
                System.out.println(result);

            } else if (choice == 3) {
                System.out.print("Enter amount: ");
                double amount = scanner.nextDouble();

                String result = service.withdraw(amount);
                System.out.println(result);

            } else if (choice == 4) {
                System.out.println("Goodbye 👋");
                break;

            } else {
                System.out.println("Invalid option!");
            }

            System.out.println();
        }

        scanner.close();
    }

    static void showMenu() {
        System.out.println("=== Payment System ===");
        System.out.println("1. Check Balance");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Exit");
    }
}
