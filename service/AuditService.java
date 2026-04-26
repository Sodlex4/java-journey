package service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class AuditService {

    private static final String AUDIT_LOG_FILE = "logs/audit.log";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static AuditService instance;
    
    private final ConcurrentHashMap<String, Long> actionCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastActionTime = new ConcurrentHashMap<>();

    public static synchronized AuditService getInstance() {
        if (instance == null) {
            instance = new AuditService();
        }
        return instance;
    }

    public void logAction(String userId, String action, String details, String result) {
        String timestamp = DATE_FORMAT.format(new Date());
        String logEntry = String.format("[%s] USER=%s ACTION=%s DETAILS=%s RESULT=%s", 
                timestamp, userId, action, details, result);
        
        try (PrintWriter pw = new PrintWriter(new FileWriter(AUDIT_LOG_FILE, true))) {
            pw.println(logEntry);
        } catch (IOException e) {
            System.err.println("Audit log write failed: " + e.getMessage());
        }
    }

    public void logLogin(String userId, String result) {
        logAction(userId, "LOGIN", "", result);
    }

    public void logTransaction(String userId, String type, double amount, String result) {
        logAction(userId, "TRANSACTION", type + ":" + amount, result);
    }

    public void logPinChange(String userId, String result) {
        logAction(userId, "PIN_CHANGE", "", result);
    }

    public void logFailedAttempt(String userId, String action, String reason) {
        logAction(userId, "FAILED_ATTEMPT", action, reason);
        actionCounts.merge(userId, 1L, Long::sum);
    }

    public long getActionCount(String userId) {
        return actionCounts.getOrDefault(userId, 0L);
    }

    public boolean isRateLimited(String userId, String action, int maxPerMinute) {
        long now = System.currentTimeMillis();
        String key = userId + ":" + action;
        Long lastTime = lastActionTime.get(key);
        
        if (lastTime == null || now - lastTime > 60000) {
            lastActionTime.put(key, now);
            return false;
        }
        return true;
    }

    public void clearRateLimit(String userId) {
        actionCounts.remove(userId);
        lastActionTime.entrySet().removeIf(e -> e.getKey().startsWith(userId + ":"));
    }
}