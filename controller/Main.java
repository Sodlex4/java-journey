package controller;

import java.util.List;
import java.util.Scanner;
import java.io.Console;
import service.PaymentService;
import service.DatabaseService;
import model.UserAccount;

public class Main {

    private static String readPin(Scanner scanner, String prompt) {
        Console console = System.console();
        if (console != null && prompt.isEmpty()) {
            char[] chars = console.readPassword("Enter PIN: ");
            if (chars != null && chars.length > 0) {
                return new String(chars);
            }
        }
        System.out.print(prompt.isEmpty() ? "Enter PIN: " : prompt);
        return scanner.nextLine();
    }

    private static boolean verifyPinWithAttempts(Scanner scanner, DatabaseService dbService, UserAccount user, int maxAttempts) {
        if (dbService.isLocked(user.getId())) {
            long remaining = dbService.getLockRemainingSeconds(user.getId());
            if (remaining > 0) {
                System.out.println("Account locked. Try again in " + remaining + " seconds.");
                return false;
            }
            dbService.unlockAccount(user.getId());
        }
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String pin = readPin(scanner, "Enter PIN: ");
            if (pin.isEmpty()) {
                System.out.println("PIN required.");
                continue;
            }
            if (dbService.verifyPin(user.getId(), pin)) {
                dbService.resetFailedAttempts(user.getId());
                return true;
            }
            int remainingAttempts = maxAttempts - attempt;
            if (remainingAttempts > 0) {
                System.out.println("Incorrect PIN. " + remainingAttempts + " attempt(s) remaining.");
            }
        }
        
        int lockCount = dbService.getLockCount(user.getId()) + 1;
        int lockDuration = lockCount <= 1 ? 30 : (lockCount == 2 ? 60 : 300);
        long lockUntil = System.currentTimeMillis() + (lockDuration * 1000L);
        dbService.lockAccount(user.getId(), lockUntil, lockCount);
        
        System.out.println("Account locked for " + lockDuration + " seconds (lock #" + lockCount + ").");
        return false;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        DatabaseService dbService = new DatabaseService();
        
        try {
            dbService.getConnection().close();
        } catch (Exception e) {
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

        UserAccount user1 = users.get(0);
        UserAccount user2 = users.size() > 1 ? users.get(1) : null;

        user1.setTransactions(dbService.getUserTransactions(user1.getId()));
        if (user2 != null) {
            user2.setTransactions(dbService.getUserTransactions(user2.getId()));
        }

        UserAccount currentUser = user1;

        while (true) {
            showMenu(currentUser);

            int choice = getValidInt(scanner, "Choice: ");

            switch (choice) {
                case 1:
                    showWalletMenu(scanner, dbService, service, currentUser, user2);
                    break;
                case 2:
                    showSavingsMenu(scanner, dbService, service, currentUser);
                    break;
                case 3:
                    showBillsMenu(scanner, dbService, service, currentUser);
                    break;
                case 4:
                    showReportsMenu(scanner, service, currentUser, safaricom);
                    break;
                case 5:
                    showSettingsMenu(scanner, currentUser, user1, user2);
                    break;
                case 0:
                    dbService.updateSystemBalance(safaricom.getBalance());
                    dbService.close();
                    scanner.close();
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option!");
            }

            System.out.println();
        }
    }

    static void showMenu(UserAccount user) {
        System.out.println("\n=== M-PESA MENU (" + user.getUsername() + ") ===");
        System.out.println("1. Wallet");
        System.out.println("2. Savings & Loans");
        System.out.println("3. Bills & Payments");
        System.out.println("4. Reports");
        System.out.println("5. Settings");
        System.out.println("0. Exit");
    }

    private static void showWalletMenu(Scanner scanner, DatabaseService dbService, 
            PaymentService service, UserAccount currentUser, UserAccount user2) {
        while (true) {
            System.out.println("\n=== WALLET ===");
            System.out.println("1. Check Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Send Money");
            System.out.println("5. Buy Airtime");
            System.out.println("0. Back");
            
            int choice = getValidInt(scanner, "Choice: ");
            if (choice == 0) return;
            
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
                    if (!verifyPinWithAttempts(scanner, dbService, currentUser, 3)) break;
                    System.out.println(service.withdraw(currentUser, withdrawAmount));
                    dbService.updateBalance(currentUser.getId(), currentUser.getBalance());
                    break;
                case 4:
                    if (user2 == null) { System.out.println("No second user."); break; }
                    double sendAmount = getValidDouble(scanner, "Enter amount: ");
                    if (!verifyPinWithAttempts(scanner, dbService, currentUser, 3)) break;
                    System.out.println(service.sendMoney(currentUser, user2, sendAmount));
                    dbService.updateBalance(currentUser.getId(), currentUser.getBalance());
                    dbService.updateBalance(user2.getId(), user2.getBalance());
                    break;
                case 5:
                    double airtime = getValidDouble(scanner, "Enter airtime amount: ");
                    if (!verifyPinWithAttempts(scanner, dbService, currentUser, 3)) break;
                    System.out.println(service.buyAirtime(currentUser, airtime));
                    dbService.updateBalance(currentUser.getId(), currentUser.getBalance());
                    break;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private static void showSavingsMenu(Scanner scanner, DatabaseService dbService,
            PaymentService service, UserAccount currentUser) {
        while (true) {
            System.out.println("\n=== SAVINGS & LOANS ===");
            System.out.println("1. Deposit to Simi");
            System.out.println("2. Withdraw from Simi");
            System.out.println("3. Check Simi Balance");
            System.out.println("4. Request Loan");
            System.out.println("0. Back");
            
            int choice = getValidInt(scanner, "Choice: ");
            if (choice == 0) return;
            
            switch (choice) {
                case 1:
                    double savingsDeposit = getValidDouble(scanner, "Enter amount: ");
                    if (!verifyPinWithAttempts(scanner, dbService, currentUser, 3)) break;
                    System.out.println(service.depositToSavings(currentUser, savingsDeposit));
                    if (savingsDeposit > 0) dbService.updateBalance(currentUser.getId(), currentUser.getBalance());
                    break;
                case 2:
                    double savingsWithdraw = getValidDouble(scanner, "Enter amount: ");
                    if (!verifyPinWithAttempts(scanner, dbService, currentUser, 3)) break;
                    System.out.println(service.withdrawFromSavings(currentUser, savingsWithdraw));
                    if (savingsWithdraw > 0) dbService.updateBalance(currentUser.getId(), currentUser.getBalance());
                    break;
                case 3:
                    System.out.println(service.checkSavingsBalance(currentUser));
                    break;
                case 4:
                    double loan = getValidDouble(scanner, "Enter loan amount: ");
                    System.out.println(service.requestLoan(currentUser, loan));
                    dbService.updateBalance(currentUser.getId(), currentUser.getBalance());
                    break;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private static void showBillsMenu(Scanner scanner, DatabaseService dbService,
            PaymentService service, UserAccount currentUser) {
        while (true) {
            System.out.println("\n=== BILLS & PAYMENTS ===");
            System.out.println("1. Lipa Na M-PESA");
            System.out.println("2. Create Bill Split");
            System.out.println("3. Pay Bill Split");
            System.out.println("4. View Bill Splits");
            System.out.println("0. Back");
            
            int choice = getValidInt(scanner, "Choice: ");
            if (choice == 0) return;
            
            switch (choice) {
                case 1:
                    System.out.print("Business name: ");
                    String business = scanner.nextLine();
                    double payAmount = getValidDouble(scanner, "Enter amount: ");
                    if (!verifyPinWithAttempts(scanner, dbService, currentUser, 3)) break;
                    System.out.println(service.lipaNaMpesa(currentUser, payAmount, business));
                    dbService.updateBalance(currentUser.getId(), currentUser.getBalance());
                    break;
                case 2:
                    String splitTitle = scanner.nextLine();
                    double splitAmount = getValidDouble(scanner, "Enter amount: ");
                    int splitCount = getValidInt(scanner, "Enter count: ");
                    if (!verifyPinWithAttempts(scanner, dbService, currentUser, 3)) break;
                    System.out.println(service.createBillSplit(currentUser, splitCount, splitAmount, splitTitle));
                    dbService.updateBalance(currentUser.getId(), currentUser.getBalance());
                    break;
                case 3:
                    int splitId = getValidInt(scanner, "Enter split ID: ");
                    if (!verifyPinWithAttempts(scanner, dbService, currentUser, 3)) break;
                    System.out.println(service.payBillSplit(currentUser, splitId));
                    dbService.updateBalance(currentUser.getId(), currentUser.getBalance());
                    break;
                case 4:
                    System.out.println(service.viewBillSplits(currentUser));
                    break;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private static void showReportsMenu(Scanner scanner, PaymentService service, 
            UserAccount currentUser, UserAccount safaricom) {
        while (true) {
            System.out.println("\n=== REPORTS ===");
            System.out.println("1. View Transactions");
            System.out.println("2. View Fees Collected");
            System.out.println("0. Back");
            
            int choice = getValidInt(scanner, "Choice: ");
            if (choice == 0) return;
            
            switch (choice) {
                case 1:
                    service.printTransactions(currentUser);
                    break;
                case 2:
                    System.out.println("Safaricom: KES " + safaricom.getBalance());
                    break;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private static void showSettingsMenu(Scanner scanner, UserAccount currentUser, 
            UserAccount user1, UserAccount user2) {
        while (true) {
            System.out.println("\n=== SETTINGS ===");
            System.out.println("1. Switch User");
            System.out.println("0. Back");
            
            int choice = getValidInt(scanner, "Choice: ");
            if (choice == 0) return;
            
            switch (choice) {
                case 1:
                    currentUser = (currentUser == user1) ? user2 : user1;
                    System.out.println("Switched to " + currentUser.getUsername());
                    break;
                default:
                    System.out.println("Invalid option!");
            }
        }
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
            if (!scanner.hasNextInt()) {
                if (!scanner.hasNextLine()) {
                    System.out.println("\nGoodbye!");
                    System.exit(0);
                }
                System.out.println("Invalid option.");
                scanner.next();
                continue;
            }
            int value = scanner.nextInt();
            scanner.nextLine();
            return value;
        }
    }
}