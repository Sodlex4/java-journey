package com.mpesa.security;

import com.mpesa.model.IdempotencyEntry;
import com.mpesa.repository.IdempotencyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyRepository repository;

    public IdempotencyFilter(IdempotencyRepository repository) {
        this.repository = repository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return !("POST".equals(method) && path.startsWith("/api/payment/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        var existing = repository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            IdempotencyEntry entry = existing.get();
            response.setStatus(entry.getResponseStatus());
            response.setContentType("application/json");
            response.getWriter().write(entry.getResponseBody());
            return;
        }

        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrapped);

            int status = wrapped.getStatus();
            if (status >= 200 && status < 300) {
                String body = new String(wrapped.getContentAsByteArray(),
                    response.getCharacterEncoding());
                repository.save(new IdempotencyEntry(key, status, body));
            }
        } catch (DataIntegrityViolationException e) {
            repository.findByIdempotencyKey(key).ifPresent(cached -> {
                wrapped.reset();
                try {
                    response.setStatus(cached.getResponseStatus());
                    response.setContentType("application/json");
                    response.getWriter().write(cached.getResponseBody());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } finally {
            wrapped.copyBodyToResponse();
        }
    }
}
