package com.mpesa.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rate_limits", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ip_address", "window_start"})
})
public class RateLimitEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "window_start", nullable = false)
    private Long windowStart;

    @Column(name = "request_count", nullable = false)
    private int requestCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public RateLimitEntry() {}

    public RateLimitEntry(String ipAddress, Long windowStart, int requestCount) {
        this.ipAddress = ipAddress;
        this.windowStart = windowStart;
        this.requestCount = requestCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public Long getWindowStart() { return windowStart; }
    public void setWindowStart(Long windowStart) { this.windowStart = windowStart; }
    public int getRequestCount() { return requestCount; }
    public void setRequestCount(int requestCount) { this.requestCount = requestCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
