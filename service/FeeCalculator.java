package service;

public class FeeCalculator {

    public double calculate(double amount) {
        if (amount <= 100) return 0;
        else if (amount <= 500) return 13;
        else if (amount <= 1000) return 25;
        else return 30;
    }
}