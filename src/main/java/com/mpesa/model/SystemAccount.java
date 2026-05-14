package com.mpesa.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "system_accounts")
public class SystemAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "account_name", unique = true, nullable = false, length = 50)
    private String accountName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    public SystemAccount() {}

    public SystemAccount(String accountName, BigDecimal balance) {
        this.accountName = accountName;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public void deposit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}
