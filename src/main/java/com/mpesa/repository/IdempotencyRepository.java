package com.mpesa.repository;

import com.mpesa.model.IdempotencyEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyEntry, Long> {
    Optional<IdempotencyEntry> findByIdempotencyKey(String idempotencyKey);
}
