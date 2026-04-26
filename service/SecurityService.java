package service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityService {

    private static final int LOCK_DURATION_1 = 30;
    private static final int LOCK_DURATION_2 = 60;
    private static final int LOCK_DURATION_3 = 300;
    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_LOGIN_ATTEMPTS_PER_MINUTE = 5;
    private static final int ACTION_COOLDOWN_MS = 60000;
    
    private final DatabaseService dbService;
    private final AuditService auditService;
    private final Map<String, Long> rateLimitTracker = new ConcurrentHashMap<>();

    public SecurityService(DatabaseService dbService) {
        this.dbService = dbService;
        this.auditService = AuditService.getInstance();
    }

    public String authenticate(int userId, String pin) {
        if (auditService.isRateLimited(String.valueOf(userId), "AUTH", MAX_LOGIN_ATTEMPTS_PER_MINUTE)) {
            auditService.logFailedAttempt(String.valueOf(userId), "AUTH", "Rate limited");
            return "ERROR: Too many attempts. Please wait.";
        }
        
        if (isLocked(userId)) {
            long remaining = getLockRemainingSeconds(userId);
            if (remaining > 0) {
                return "LOCKED: Try again in " + remaining + " seconds";
            }
        }

        if (!dbService.verifyPin(userId, pin)) {
            dbService.incrementFailedAttempts(userId);
            int attempts = dbService.getFailedAttempts(userId);
            int remaining = MAX_ATTEMPTS - attempts;
            auditService.logFailedAttempt(String.valueOf(userId), "AUTH", "Invalid PIN");
            
            if (remaining <= 0) {
                lockAccount(userId);
                return "LOCKED: Too many attempts";
            }
            return "ERROR: Incorrect PIN. " + remaining + " attempt(s) left";
        }

        dbService.resetFailedAttempts(userId);
        auditService.logLogin(String.valueOf(userId), "SUCCESS");
        return "SUCCESS";
    }

    public boolean isLocked(int userId) {
        if (!dbService.isLocked(userId)) {
            return false;
        }
        return checkLockExpired(userId);
    }

    private boolean checkLockExpired(int userId) {
        long lockUntil = dbService.getLockUntil(userId);
        if (lockUntil == 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now >= lockUntil) {
            unlockAccount(userId);
            return false;
        }
        return true;
    }

    public long getLockRemainingSeconds(int userId) {
        long lockUntil = dbService.getLockUntil(userId);
        if (lockUntil == 0) {
            return 0;
        }
        long now = System.currentTimeMillis();
        long remaining = (lockUntil - now) / 1000;
        if (remaining <= 0) {
            unlockAccount(userId);
            return 0;
        }
        return remaining;
    }

    private void lockAccount(int userId) {
        int lockCount = dbService.getLockCount(userId) + 1;
        int duration = getLockDuration(lockCount);
        long lockUntil = System.currentTimeMillis() + (duration * 1000);
        dbService.lockAccount(userId, lockUntil, lockCount);
        auditService.logAction(String.valueOf(userId), "ACCOUNT_LOCKED", "count=" + lockCount, "locked for " + duration + "s");
    }

    public void unlockAccount(int userId) {
        dbService.unlockAccount(userId);
        auditService.logAction(String.valueOf(userId), "ACCOUNT_UNLOCKED", "", "unlocked");
    }

    private int getLockDuration(int lockCount) {
        if (lockCount <= 1) return LOCK_DURATION_1;
        if (lockCount == 2) return LOCK_DURATION_2;
        return LOCK_DURATION_3;
    }
}
