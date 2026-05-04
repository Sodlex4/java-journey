package com.mpesa;

import com.mpesa.security.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class RateLimitFilterTest {

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Test
    public void testRateLimitFilterExists() {
        assertNotNull(rateLimitFilter);
    }
}