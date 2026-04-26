package com.mpesa;

import com.mpesa.security.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RateLimitFilterTest {

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Test
    public void testRateLimitFilterExists() {
        assertNotNull(rateLimitFilter);
    }
}