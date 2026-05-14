package com.mpesa.security;

import com.mpesa.service.RateLimitService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RateLimitFilter implements Filter {

    private final RateLimitService rateLimitService;
    private final int maxRequestsPerMinute;

    public RateLimitFilter(RateLimitService rateLimitService,
                           @Value("${app.rate-limit.max-requests:60}") int maxRequestsPerMinute) {
        this.rateLimitService = rateLimitService;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIp(httpRequest);

        if (!rateLimitService.tryAcquire(clientIp, maxRequestsPerMinute)) {
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                "{\"error\":\"Rate limit exceeded. Try again later.\"," +
                "\"errorCode\":\"RATE_LIMITED\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}