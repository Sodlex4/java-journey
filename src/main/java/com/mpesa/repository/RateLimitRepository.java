package com.mpesa.repository;

import com.mpesa.model.RateLimitEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RateLimitRepository extends JpaRepository<RateLimitEntry, Long> {

    Optional<RateLimitEntry> findByIpAddressAndWindowStart(String ipAddress, Long windowStart);

    @Modifying
    @Query(value = "INSERT INTO rate_limits (ip_address, window_start, request_count) "
                 + "VALUES (:ip, :window, 1) ON DUPLICATE KEY UPDATE request_count = request_count + 1",
           nativeQuery = true)
    void insertOrIncrement(@Param("ip") String ip, @Param("window") Long window);

    @Modifying
    @Query(value = "DELETE FROM rate_limits WHERE window_start < :cutoff", nativeQuery = true)
    void deleteByWindowStartBefore(@Param("cutoff") Long cutoff);
}
