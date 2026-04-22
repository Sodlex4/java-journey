package logic;

import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {

    private final String id;
    private final String type;
    private final double amount;
    private final String status;
    private final String fromUser;
    private final String toUser;
    private final LocalDateTime timestamp;

    public Transaction(String type, double amount, String status, String fromUser, String toUser) {
        this.id = generateId();
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.timestamp = LocalDateTime.now();
    }

    private String generateId() {
        return "MPESA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return timestamp +
                " | " + type +
                " | KES " + amount +
                " | " + status +
                " | TXID: " + id +
                (fromUser != null ? " | From: " + fromUser : "") +
                (toUser != null ? " | To: " + toUser : "");
    }
}
