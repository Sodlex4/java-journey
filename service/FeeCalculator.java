package service;

import java.io.*;
import java.util.Properties;

public class FeeCalculator {

    private static double TIER_1 = 100;
    private static double TIER_2 = 500;
    private static double TIER_3 = 1000;
    private static double RATE_1 = 0;
    private static double RATE_2 = 13;
    private static double RATE_3 = 25;
    private static double RATE_4 = 30;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(".env")) {
            props.load(in);
            TIER_1 = Double.parseDouble(props.getProperty("FEE_TIER_1", "100"));
            TIER_2 = Double.parseDouble(props.getProperty("FEE_TIER_2", "500"));
            TIER_3 = Double.parseDouble(props.getProperty("FEE_TIER_3", "1000"));
            RATE_1 = Double.parseDouble(props.getProperty("FEE_RATE_1", "0"));
            RATE_2 = Double.parseDouble(props.getProperty("FEE_RATE_2", "13"));
            RATE_3 = Double.parseDouble(props.getProperty("FEE_RATE_3", "25"));
            RATE_4 = Double.parseDouble(props.getProperty("FEE_RATE_4", "30"));
        } catch (IOException | NumberFormatException e) {
        }
    }

    public double calculate(double amount) {
        if (amount <= TIER_1) return RATE_1;
        else if (amount <= TIER_2) return RATE_2;
        else if (amount <= TIER_3) return RATE_3;
        else return RATE_4;
    }
}