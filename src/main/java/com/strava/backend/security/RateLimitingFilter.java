package com.strava.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int LOGIN_MAX_REQUESTS = 10;
    private static final int REGISTER_MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 60_000; // 1 minute

    private final ConcurrentHashMap<String, List<Long>> requestCounts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.equals("/api/v1/auth/login") && !path.equals("/api/v1/auth/register");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        String path = request.getServletPath();
        String key = clientIp + ":" + path;

        int maxRequests = path.equals("/api/v1/auth/register") ? REGISTER_MAX_REQUESTS : LOGIN_MAX_REQUESTS;

        List<Long> timestamps = requestCounts.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        long now = System.currentTimeMillis();

        // Remove expired timestamps
        timestamps.removeIf(ts -> now - ts > WINDOW_MS);

        if (timestamps.size() >= maxRequests) {
            log.warn("Rate limit exceeded for IP {} on {}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    Map.of("error", "Too many requests. Please try again later."));
            return;
        }

        timestamps.add(now);
        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Scheduled(fixedRate = 300_000) // every 5 minutes
    public void cleanup() {
        long now = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(ts -> now - ts > WINDOW_MS);
            return entry.getValue().isEmpty();
        });
    }
}
