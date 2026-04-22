package controller;

import java.util.Scanner;
import service.PaymentService;
import model.UserAccount;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        // 🔹 Safaricom system account
        UserAccount safaricom = new UserAccount("M-PESA", 0);

        // 🔹 Initialize service with Safaricom account
        PaymentService service = new PaymentService(safaricom);


      // 🔹 Create users dynamically (no hardcoding)

System.out.print("Enter initial balance for john: ");
double johnBalance = getValidDouble(scanner, "");

System.out.print("Enter initial balance for mary: ");
double maryBalance = getValidDouble(scanner, "");

// 🔹 Two user accounts (now dynamic)
UserAccount user1 = new UserAccount("john", johnBalance);
UserAccount user2 = new UserAccount("mary", maryBalance);

// 🔹 Default current user
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
                    break;

                case 3:
                    double withdrawAmount = getValidDouble(scanner, "Enter amount: ");
                    System.out.println(service.withdraw(currentUser, withdrawAmount));
                    break;

                case 4:
                    UserAccount receiver = (currentUser == user1) ? user2 : user1;
                    double sendAmount = getValidDouble(scanner, "Enter amount to send: ");
                    System.out.println(service.sendMoney(currentUser, receiver, sendAmount));
                    break;

                case 5:
                    double airtime = getValidDouble(scanner, "Enter airtime amount: ");
                    System.out.println(service.buyAirtime(currentUser, airtime));
                    break;

                case 6:
                    System.out.print("Enter business name: ");
                    String business = scanner.nextLine();
                    double payAmount = getValidDouble(scanner, "Enter amount to pay: ");
                    System.out.println(service.lipaNaMpesa(currentUser, payAmount, business));
                    break;

                case 7:
                    double loan = getValidDouble(scanner, "Enter loan amount: ");
                    System.out.println(service.requestLoan(currentUser, loan));
                    break;

                case 8:
                    currentUser = (currentUser == user1) ? user2 : user1;
                    System.out.println("🔄 Switched to " + currentUser.getUsername());
                    break;

                case 9:
                    service.printTransactions(currentUser);
                    break;

                case 10:
                    System.out.println("Safaricom account balance: KES " + safaricom.getBalance());
                      break;

                      case 11:
                    System.out.println("Goodbye 👋");
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
