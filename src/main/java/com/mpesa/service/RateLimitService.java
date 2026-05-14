package com.mpesa.service;

import com.mpesa.repository.RateLimitRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RateLimitService {

    private final RateLimitRepository repository;

    public RateLimitService(RateLimitRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean tryAcquire(String ipAddress, int maxRequestsPerMinute) {
        long windowStart = System.currentTimeMillis() / 60000;

        try {
            repository.insertOrIncrement(ipAddress, windowStart);
        } catch (DataIntegrityViolationException e) {
            repository.insertOrIncrement(ipAddress, windowStart);
        }

        return repository.findByIpAddressAndWindowStart(ipAddress, windowStart)
            .map(entry -> entry.getRequestCount() <= maxRequestsPerMinute)
            .orElse(false);
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanup() {
        long cutoff = System.currentTimeMillis() / 60000 - 10;
        repository.deleteByWindowStartBefore(cutoff);
    }
}
