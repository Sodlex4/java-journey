package com.mpesa.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "tx_id", unique = true, length = 20)
    private String txId;
    
    @Column(length = 20)
    private String type;
    
    private BigDecimal amount;
    
    @Column(length = 20)
    private String status;
    
    @Column(name = "from_user")
    private String fromUser;
    
    @Column(name = "to_user")
    private String toUser;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    private LocalDateTime timestamp = LocalDateTime.now();
    
    public Transaction() {}
    
    public Transaction(String txId, String type, BigDecimal amount, String status, String fromUser, String toUser) {
        this.txId = txId;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.fromUser = fromUser;
        this.toUser = toUser;
    }
    
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTxId() { return txId; }
    public void setTxId(String txId) { this.txId = txId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFromUser() { return fromUser; }
    public void setFromUser(String fromUser) { this.fromUser = fromUser; }
    public String getToUser() { return toUser; }
    public void setToUser(String toUser) { this.toUser = toUser; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
