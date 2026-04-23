package service;

public class SecurityService {

    private static final int LOCK_DURATION_1 = 30;
    private static final int LOCK_DURATION_2 = 60;
    private static final int LOCK_DURATION_3 = 300;
    private static final int MAX_ATTEMPTS = 3;
    
    private final DatabaseService dbService;

    public SecurityService(DatabaseService dbService) {
        this.dbService = dbService;
    }

    public String authenticate(int userId, String pin) {
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
            
            if (remaining <= 0) {
                lockAccount(userId);
                return "LOCKED: Too many attempts";
            }
            return "ERROR: Incorrect PIN. " + remaining + " attempt(s) left";
        }

        dbService.resetFailedAttempts(userId);
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
    }

    public void unlockAccount(int userId) {
        dbService.unlockAccount(userId);
    }

    private int getLockDuration(int lockCount) {
        if (lockCount <= 1) return LOCK_DURATION_1;
        if (lockCount == 2) return LOCK_DURATION_2;
        return LOCK_DURATION_3;
    }
}
