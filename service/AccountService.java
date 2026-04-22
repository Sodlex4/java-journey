package service;

import model.UserAccount;

public class AccountService {

    private final UserAccount safaricom;

    public AccountService(UserAccount safaricom) {
        this.safaricom = safaricom;
    }

    public boolean deposit(UserAccount user, double amount) {
        if (amount <= 0) {
            return false;
        }
        user.deposit(amount);
        return true;
    }

    public boolean withdraw(UserAccount user, double amount, double fee) {
        if (amount <= 0) {
            return false;
        }
        double totalDeduct = amount + fee;
        if (!user.withdraw(totalDeduct)) {
            return false;
        }
        if (fee > 0) {
            safaricom.deposit(fee);
        }
        return true;
    }

    public double checkBalance(UserAccount user) {
        return user.getBalance();
    }

    public UserAccount getSafaricom() {
        return safaricom;
    }
}