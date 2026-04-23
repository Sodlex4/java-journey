package controller;

import java.util.List;
import java.util.Scanner;
import service.PaymentService;
import service.DatabaseService;
import model.UserAccount;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        DatabaseService dbService = new DatabaseService();

        if (dbService.getConnection() == null) {
            System.err.println("Cannot connect to database. Exiting.");
            return;
        }

        List<UserAccount> users = dbService.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("No users in database. Creating default users...");
            dbService.createUser("john", 1000.00);
            dbService.createUser("mary", 500.00);
            users = dbService.getAllUsers();
        }

        UserAccount safaricom = new UserAccount(0, "M-PESA", dbService.getSystemBalance());
        PaymentService service = new PaymentService(safaricom, dbService);

        System.out.println("\n=== Loaded users from database ===");
        for (UserAccount u : users) {
            System.out.println("- " + u.getUsername() + " (KES " + u.getBalance() + ")");
        }
        System.out.println();

        UserAccount user1 = users.get(0);
        UserAccount user2 = users.size() > 1 ? users.get(1) : null;

        if (user2 == null) {
            System.out.println("Only one user in DB. Creating second user...");
            dbService.createUser("mary", 500.00);
            user2 = dbService.getUserByUsername("mary");
        }

        user1.setTransactions(dbService.getUserTransactions(user1.getId()));
        if (user2 != null) {
            user2.setTransactions(dbService.getUserTransactions(user2.getId()));
        }

        UserAccount currentUser = user1;

        while (true) {
            showMenu(currentUser);

            int choice = getValidInt(scanner, "Choose option: ");

            switch (choice) {
                case 1:
                    System.out.println("Balance: KES " + service.checkBalance(currentUser));
                    break;

                case 2:
                    double depositAmount = getValidDouble(scanner, "Enter amount: ");
                    System.out.println(service.deposit(currentUser, depositAmount));
                    dbService.updateBalance(currentUser.getId(), currentUser.getBalance());
                    break;

                case 3:
                    double withdrawAmount = getValidDouble(scanner, "Enter amount: ");
                    System.out.println(service.withdraw(currentUser, withdrawAmount));
                    dbService.updateBalance(currentUser.getId(), currentUser.getBalance());
                    break;

                case 4:
                    if (user2 == null) {
                        System.out.println("No second user to send to.");
                        break;
                    }
                    double sendAmount = getValidDouble(scanner, "Enter amount to send: ");
                    System.out.println(service.sendMoney(currentUser, user2, sendAmount));
                    dbService.updateBalance(currentUser.getId(), currentUser.getBalance());
                    dbService.updateBalance(user2.getId(), user2.getBalance());
                    break;

                case 5:
                    double airtime = getValidDouble(scanner, "Enter airtime amount: ");
                    System.out.println(service.buyAirtime(currentUser, airtime));
                    dbService.updateBalance(currentUser.getId(), currentUser.getBalance());
                    break;

                case 6:
                    System.out.print("Enter business name: ");
                    String business = scanner.nextLine();
                    double payAmount = getValidDouble(scanner, "Enter amount to pay: ");
                    System.out.println(service.lipaNaMpesa(currentUser, payAmount, business));
                    dbService.updateBalance(currentUser.getId(), currentUser.getBalance());
                    break;

                case 7:
                    double loan = getValidDouble(scanner, "Enter loan amount: ");
                    System.out.println(service.requestLoan(currentUser, loan));
                    dbService.updateBalance(currentUser.getId(), currentUser.getBalance());
                    break;

                case 8:
                    currentUser = (currentUser == user1) ? user2 : user1;
                    System.out.println("Switchped to " + currentUser.getUsername());
                    break;

                case 9:
                    service.printTransactions(currentUser);
                    break;

                case 10:
                    System.out.println("Safaricom account balance: KES " + safaricom.getBalance());
                    break;

                case 11:
                    dbService.updateSystemBalance(safaricom.getBalance());
                    System.out.println("Goodbye!");
                    dbService.close();
                    scanner.close();
                    return;

                default:
                    System.out.println("Invalid option!");
            }

            System.out.println();
        }
    }

    static void showMenu(UserAccount user) {
        System.out.println("=== M-PESA MENU (" + user.getUsername() + ") ===");
        System.out.println("1. Check Balance");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Send Money");
        System.out.println("5. Buy Airtime");
        System.out.println("6. Lipa Na M-PESA");
        System.out.println("7. Request Loan");
        System.out.println("8. Switch User");
        System.out.println("9. View Transactions");
        System.out.println("10. View Safaricom fees collected");
        System.out.println("11. Exit");
    }

    public static double getValidDouble(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            if (scanner.hasNextDouble()) {
                double value = scanner.nextDouble();
                scanner.nextLine();
                return value;
            } else {
                System.out.println("Invalid input.");
                scanner.next();
            }
        }
    }

    public static int getValidInt(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            if (scanner.hasNextInt()) {
                int value = scanner.nextInt();
                scanner.nextLine();
                return value;
            } else {
                System.out.println("Invalid option.");
                scanner.next();
            }
        }
    }
}